package modelos;

import static java.lang.Thread.sleep;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import micelaneos.*;
import controlador.*;

public class CPU extends Thread {
    private int quantum;
    private int memoryAddressRegister;
    private int programCounter;
    private List interruptionsList;
    private Planificador planificador;
    private Proceso currentProcess;
    private int id;
    private Semaphore mutexInterruciones;
    private Semaphore mutexCPUs;
    private ControladorSimulacion controlador;
    private PerformanceMetrics metrics;

    public CPU(ControladorSimulacion controlador, Planificador planner, int id, Semaphore mutexCPUs, PerformanceMetrics metrics) {
        this.controlador = controlador;
        this.planificador = planner;
        this.id = id;
        this.mutexCPUs = mutexCPUs;
        this.mutexInterruciones = new Semaphore(1);
        this.interruptionsList = new List();
        this.metrics = metrics;
    }  

    public int getQuantum() {
        return quantum;
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }

    public int getMemoryAddressRegister() {
        return memoryAddressRegister;
    }

    public void setMemoryAddressRegister(int memoryAddressRegister) {
        this.memoryAddressRegister = memoryAddressRegister;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
    }

    public List getInterruptionsList() {
        return interruptionsList;
    }

    public void setInterruptionsList(List interruptionsList) {
        this.interruptionsList = interruptionsList;
    }

    public Planificador getPlanificador() {
        return planificador;
    }

    public void setPlanificador(Planificador planificador) {
        this.planificador = planificador;
    }

    public Proceso getCurrentProcess() {
        return currentProcess;
    }

    public void setCurrentProcess(Proceso currentProcess) {
        this.currentProcess = currentProcess;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Semaphore getMutexInterruciones() {
        return mutexInterruciones;
    }

    public void setMutexInterruciones(Semaphore mutexInterruciones) {
        this.mutexInterruciones = mutexInterruciones;
    }

    public Semaphore getMutexCPUs() {
        return mutexCPUs;
    }

    public void setMutexCPUs(Semaphore mutexCPUs) {
        this.mutexCPUs = mutexCPUs;
    }

    public ControladorSimulacion getControlador() {
        return controlador;
    }

    public void setControlador(ControladorSimulacion controlador) {
        this.controlador = controlador;
    }
    
    @Override
    public void run(){
        this.obtenerProceso();
        while(true){
            if(this.interruptionsList.isEmpty()){
                Interrupcion exception = (Interrupcion) interruptionsList.getHead().getValue();
                interruptionsList.delete(interruptionsList.getHead());
                this.interruptHandler(exception);
            }else{
                if (planificador.getSelectedAlgorithm() == 1 && this.quantum<=0 && planificador.getReadyList().isEmpty()) {
                    this.usarPlanificador("Listo");
                    this.obtenerProceso();
                }else if(planificador.getSelectedAlgorithm() == 3 && this.checkSRT() && planificador.getReadyList().isEmpty()){
                    this.usarPlanificador("Listo");
                    this.obtenerProceso();
                }else{
                    if(this.currentProcess.getInstrucciones() < this.memoryAddressRegister){
                        currentProcess.setTiempoFinalizacion(controlador.getRelojGlobal());
                        metrics.recordProcessCompletion(currentProcess);
                        this.usarPlanificador("Terminado");
                        this.obtenerProceso();
                    }else{
                        try {
                            sleep(controlador.getTiempo());
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Interrupcion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        if(currentProcess.isPrimerEjecucion()){
                            currentProcess.setTiempoInicio(controlador.getRelojGlobal());
                            currentProcess.setTiempoRespuesta(currentProcess.getTiempoInicio());
                            currentProcess.setPrimerEjecucion(false);
                        }
                        
                        this.actulizarCPUvista();
                        controlador.updateDataset(id, "Usuario");
                        metrics.incrementCpuTime();
                        quantum--;
                        
                        if(this.isInterruption(memoryAddressRegister)&& "I/O Bound".equals(this.currentProcess.getTipo())){
                            this.usarPlanificador("Bloqueado");
                            this.obtenerProceso();
                        }else{
                            programCounter++;
                            this.memoryAddressRegister++;
                            this.actulizarCPUvista();
                        }    
                    }
                }
            }
        }
    }
    
    public boolean isInterruption (int mar){
        if(mar%currentProcess.getCiclosParaExcepcion()==0 && currentProcess.getCiclosParaExcepcion() > 0){
            Interrupcion exception = new Interrupcion(id,currentProcess.getCiclosParaSatisfacerExcepcion(),this.controlador,this.currentProcess.getId(),this.interruptionsList,this.mutexInterruciones);
            exception.start();
            return true;
        }
        return false;
    }
    
    private void interruptHandler(Interrupcion exception){
        try {
            mutexCPUs.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(Interrupcion.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.planificador.updateBlockToReady(exception.getProcessId());
        mutexCPUs.release();
    }
    
    private void usarPlanificador(String state){
        try {
            mutexCPUs.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(CPU.class.getName()).log(Level.SEVERE, null, ex);
        }

        if(quantum != 5){
            this.planificador.updatePCB(currentProcess, programCounter, memoryAddressRegister, state);
        }else{
            this.planificador.updatePCB(currentProcess, state);
        }

        mutexCPUs.release();
    }
    
    private boolean checkSRT(){
        try {
            mutexCPUs.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(Interrupcion.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        boolean output = this.planificador.ifSRT(currentProcess);
        
        mutexCPUs.release();
        return output;
    }
    
    private void actulizarCPUvista(){
        String display = "Id: " + currentProcess.getId() + 
                "\nNombre: " + currentProcess.getNombre() +
                "\nEstado: " + currentProcess.getEstado() + 
                "\nPC: " + programCounter + 
                "\nMAR: " + this.memoryAddressRegister ;
        this.controlador.setCPUText(id,display );
    }
    
    private void obtenerProceso(){
        currentProcess = null;
        while(currentProcess==null){
            this.controlador.setCPUText(id,"Planificador");
            for (int i = 0; i < 4; i++) {
                try {
                    sleep(controlador.getTiempo());
                } catch (InterruptedException ex) {
                    Logger.getLogger(CPU.class.getName()).log(Level.SEVERE, null, ex);
                }
                controlador.updateDataset(id, "Sistema Operativo");
                metrics.incrementSystemTime();
            }
            try {
                mutexCPUs.acquire();
            } catch (InterruptedException ex) {
                Logger.getLogger(Interrupcion.class.getName()).log(Level.SEVERE, null, ex);
            }
            this.currentProcess = this.planificador.getProcess();
            mutexCPUs.release();
            if(currentProcess != null) break;
            this.controlador.setCPUText(id,"System 32");
            try {
                sleep(controlador.getTiempo());
            } catch (InterruptedException ex) {
                Logger.getLogger(CPU.class.getName()).log(Level.SEVERE, null, ex);
            }
            controlador.updateDataset( id, "Sistema Operativo");
            metrics.incrementSystemTime();
            if(this.interruptionsList.isEmpty()){
                Interrupcion exception = (Interrupcion) interruptionsList.getHead().getValue();
                interruptionsList.delete(interruptionsList.getHead());
                this.interruptHandler(exception);
            }
        }
        
        quantum = 5;
        programCounter = currentProcess.getPc()+1;
        memoryAddressRegister = currentProcess.getPc();
        this.actulizarCPUvista();
    }
}