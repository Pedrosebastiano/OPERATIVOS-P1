package modelos;

import controlador.ControladorSimulacion;
import java.util.Comparator;
import micelaneos.*;

public class Planificador {
    private List readyList;
    private List blockedList;
    private List exitList;
    private List allProcessList;
    private List suspendedReadyList;
    private List suspendedBlockedList;
    private ControladorSimulacion controlador;
    private EventLogger logger;
    public int selectedAlgorithm;
    private MemoryManager memoryManager;
    
    private static final int LOW_MEMORY_THRESHOLD = 50;
    private static final int RESUME_MEMORY_THRESHOLD = 100;

    public Planificador(List readyList, List blockedList, List exitList, List allProcess, 
                       List suspReadyList, List suspBlockList, ControladorSimulacion controlador) {
        this.controlador = controlador;
        this.readyList = new List();
        this.blockedList = blockedList;
        this.exitList = exitList;
        this.allProcessList = allProcess;
        this.suspendedReadyList = suspReadyList;
        this.suspendedBlockedList = suspBlockList;
        this.logger = new EventLogger();
        this.memoryManager = new MemoryManager(500);
        
        initializeProcessMemory(readyList);
        logger.logEvent("Sistema iniciado. Memoria total: " + memoryManager.getTotalMemory() + 
                       " MB. Memoria disponible: " + memoryManager.getAvailableMemory() + " MB");
    }

    private void initializeProcessMemory(List initialReadyList) {
        Nodo current = initialReadyList.getHead();
        while (current != null) {
            Proceso p = (Proceso) current.getValue();
            Nodo next = current.getpNext();
            
            if (memoryManager.canAllocate(p.getMemoriaRequerida())) {
                memoryManager.allocate(p.getMemoriaRequerida());
                p.setInMemory(true);
                p.setEstado("Listo");
                readyList.appendLast(p);
                logger.logEvent("INICIALIZACIÓN: Proceso " + p.getId() + " (" + p.getNombre() + 
                              ") cargado en memoria. Memoria asignada: " + p.getMemoriaRequerida() + 
                              " MB. Disponible: " + memoryManager.getAvailableMemory() + " MB");
            } else {
                p.suspender();
                p.setInMemory(false);
                suspendedReadyList.appendLast(p);
                logger.logEvent("INICIALIZACIÓN: Proceso " + p.getId() + " (" + p.getNombre() + 
                              ") suspendido por falta de memoria. Requiere: " + p.getMemoriaRequerida() + 
                              " MB. Disponible: " + memoryManager.getAvailableMemory() + " MB");
            }
            
            current = next;
        }
        
        updateAllLists();
    }

    public int getSelectedAlgorithm() {
        return selectedAlgorithm;
    }

    public List getReadyList() {
        return readyList;
    }

    public void setSelectedAlgorithm(int selectedAlgorithm) {
        this.selectedAlgorithm = selectedAlgorithm;
    }
    
    public EventLogger getLogger() {
        return logger;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }
    
    public Proceso getProcess(){
        Proceso output = null;
        
        checkMemoryAndResume();
        
        if(this.readyList.isEmpty()){
            if(selectedAlgorithm != controlador.getPolitica()){
                selectedAlgorithm = controlador.getPolitica();
                sortReadyQueue(selectedAlgorithm);
            }
            
            Nodo pAux = this.readyList.getHead();
            this.readyList.delete(pAux);
            output = (Proceso) pAux.getValue();
            output.setEstado("Ejecucion");
            
            if (!output.isInMemory()) {
                if (memoryManager.allocate(output.getMemoriaRequerida())) {
                    output.setInMemory(true);
                    logger.logEvent("MEMORIA: Proceso " + output.getId() + " (" + output.getNombre() + ") cargado en memoria");
                } else {
                    logger.logEvent("ERROR: No hay memoria suficiente para el proceso " + output.getId());
                }
            }
            
            logger.logEvent("SCHEDULER: Proceso " + output.getNombre() + " (ID: " + output.getId() + ") seleccionado para ejecución");
        }
        
        this.updateReadyList();
        this.updateProcessList();
        
        return output;    
    }

    private void checkMemoryAndResume() {
        Nodo current = suspendedReadyList.getHead();
        while (current != null) {
            Proceso p = (Proceso) current.getValue();
            Nodo next = current.getpNext();
            
            if (memoryManager.canAllocate(p.getMemoriaRequerida())) {
                suspendedReadyList.delete(current);
                p.reactivar();
                p.setInMemory(true);
                readyList.appendLast(p);
                memoryManager.allocate(p.getMemoriaRequerida());
                logger.logEvent("REACTIVADO: Proceso " + p.getNombre() + " (ID: " + p.getId() + 
                              ") reactivado de Suspendido-Listo. Memoria asignada: " + 
                              p.getMemoriaRequerida() + " MB. Disponible: " + 
                              memoryManager.getAvailableMemory() + " MB");
            }
            current = next;
        }

        current = suspendedBlockedList.getHead();
        while (current != null) {
            Proceso p = (Proceso) current.getValue();
            Nodo next = current.getpNext();
            
            if (memoryManager.canAllocate(p.getMemoriaRequerida())) {
                suspendedBlockedList.delete(current);
                p.reactivar();
                p.setInMemory(true);
                blockedList.appendLast(p);
                memoryManager.allocate(p.getMemoriaRequerida());
                logger.logEvent("REACTIVADO: Proceso " + p.getNombre() + " (ID: " + p.getId() + 
                              ") reactivado de Suspendido-Bloqueado. Memoria asignada: " + 
                              p.getMemoriaRequerida() + " MB. Disponible: " + 
                              memoryManager.getAvailableMemory() + " MB");
            }
            current = next;
        }
    }

    private void checkMemoryAndSuspend() {
        if (memoryManager.getAvailableMemory() < LOW_MEMORY_THRESHOLD) {
            logger.logEvent("=== MEMORIA BAJA: " + memoryManager.getAvailableMemory() + 
                          " MB disponibles. Iniciando suspensión... ===");
            
            Nodo pAux = readyList.getHead();
            while (pAux != null && memoryManager.getAvailableMemory() < RESUME_MEMORY_THRESHOLD) {
                Proceso process = (Proceso) pAux.getValue();
                Nodo next = pAux.getpNext();
                
                if (process.getMemoriaRequerida() > 0 && process.isInMemory()) {
                    readyList.delete(pAux);
                    process.suspender();
                    process.setInMemory(false);
                    suspendedReadyList.appendLast(process);
                    memoryManager.deallocate(process.getMemoriaRequerida());
                    logger.logEvent("SUSPENDIDO: Proceso " + process.getId() + " (" + process.getNombre() + 
                                  ") suspendido (Listo). Liberados " + process.getMemoriaRequerida() + 
                                  " MB. Disponible: " + memoryManager.getAvailableMemory() + " MB");
                }
                pAux = next;
            }
            
            pAux = blockedList.getHead();
            while (pAux != null && memoryManager.getAvailableMemory() < RESUME_MEMORY_THRESHOLD) {
                Proceso process = (Proceso) pAux.getValue();
                Nodo next = pAux.getpNext();
                
                if (process.getMemoriaRequerida() > 0 && process.isInMemory()) {
                    blockedList.delete(pAux);
                    process.suspender();
                    process.setInMemory(false);
                    suspendedBlockedList.appendLast(process);
                    memoryManager.deallocate(process.getMemoriaRequerida());
                    logger.logEvent("SUSPENDIDO: Proceso " + process.getId() + " (" + process.getNombre() + 
                                  ") suspendido (Bloqueado). Liberados " + process.getMemoriaRequerida() + 
                                  " MB. Disponible: " + memoryManager.getAvailableMemory() + " MB");
                }
                pAux = next;
            }
            
            logger.logEvent("=== Suspensión completada. Memoria disponible: " + 
                          memoryManager.getAvailableMemory() + " MB ===");
            updateSuspendedLists();
        }
    }
    
    private void sortReadyQueue(int schedulingAlgorithm) {
        switch (schedulingAlgorithm) {
            case 0:
                readyList = sortByWaitingTime(readyList);
                logger.logEvent("Cambio de algoritmo a FCFS");
                break;
            case 1:
                readyList = sortByWaitingTime(readyList);
                logger.logEvent("Cambio de algoritmo a Round Robin");
                break;
            case 2:
                readyList = sortByDuration(readyList);
                logger.logEvent("Cambio de algoritmo a SPN");
                break;
            case 3:
                readyList = sortByRemainingTime(readyList);
                logger.logEvent("Cambio de algoritmo a SRT");
                break;
            case 4:
                readyList = sortByHRR(readyList);
                logger.logEvent("Cambio de algoritmo a HRRN");
                break;
            case 5:
                readyList = sortByPriority(readyList);
                logger.logEvent("Cambio de algoritmo a Prioridad");
                break;
        }
    }

    private List sortByWaitingTime(List list) {
        return bubbleSort(list, (p1, p2) -> Integer.compare(((Proceso) p2).getTiempoEspera(), ((Proceso) p1).getTiempoEspera()));
    }
    
    private List sortByDuration(List list) {
        return bubbleSort(list, (p1, p2) -> Integer.compare(((Proceso) p1).getInstrucciones(), ((Proceso) p2).getInstrucciones()));
    }

    private List sortByRemainingTime(List list) {
        return bubbleSort(list, (p1, p2) -> Integer.compare(
                ((Proceso) p1).getInstrucciones() - ((Proceso) p1).getPc(),
                ((Proceso) p2).getInstrucciones() - ((Proceso) p2).getPc()
        ));
    }

    private List sortByHRR(List list) {
        return bubbleSort(list, (p1, p2) -> Double.compare(getHRR((Proceso) p2), getHRR((Proceso) p1)));
    }

    private List sortByPriority(List list) {
        return bubbleSort(list, (p1, p2) -> Integer.compare(
            ((Proceso) p1).getPrioridad(),
            ((Proceso) p2).getPrioridad()
        ));
    }

    private double getHRR(Proceso p) {
        int tiempoServicio = p.getInstrucciones();
        if(tiempoServicio == 0) return 0;
        return (p.getTiempoEspera() + tiempoServicio) / (double) tiempoServicio;
    }

    private List bubbleSort(List list, Comparator comparator) {
        if (list.getSize() <= 1) return list;

        boolean swapped;
        do {
            swapped = false;
            Nodo current = list.getHead();
            while (current != null && current.getpNext() != null) {
                if (comparator.compare(current.getValue(), current.getpNext().getValue()) > 0) {
                    Object temp = current.getValue();
                    current.setValue(current.getpNext().getValue());
                    current.getpNext().setValue(temp);
                    swapped = true;
                }
                current = current.getpNext();
            }
        } while (swapped);

        return list;
    }

    public boolean ifSRT(Proceso process){
        if(controlador.getPolitica() == 3 && this.readyList.isEmpty()){
            int currentRemainingTime = process.getInstrucciones() - process.getMar();
            
            Nodo current = this.readyList.getHead();
            while (current != null) {
                Proceso readyProcess = (Proceso) current.getValue();
                int readyRemainingTime = readyProcess.getInstrucciones() - readyProcess.getMar();
                
                if (readyRemainingTime < currentRemainingTime) {
                    return true;
                }
                current = current.getpNext();
            }    
        }
        return false;
    }

    public void updatePCB(Proceso process, int programCounter, int memoryAddressRegister, String state) {
        process.setEstado(state);
        process.setPc(programCounter);
        process.setMar(memoryAddressRegister);
        process.setTiempoEspera(0);

        handleStateTransition(process, state);
    }

    public void updatePCB(Proceso process, String state) {
        process.setEstado(state);
        process.setTiempoEspera(0);

        handleStateTransition(process, state);
    }

    private void handleStateTransition(Proceso process, String state) {
        switch (state) {
            case "Bloqueado":
                checkMemoryAndSuspend();
                
                if (!memoryManager.canAllocate(process.getMemoriaRequerida()) || 
                    memoryManager.getAvailableMemory() < LOW_MEMORY_THRESHOLD) {
                    process.suspender();
                    process.setInMemory(false);
                    suspendedBlockedList.appendLast(process);
                    memoryManager.deallocate(process.getMemoriaRequerida());
                    logger.logEvent("SUSPENDIDO: Proceso " + process.getNombre() + " (ID: " + process.getId() + 
                                  ") suspendido al bloquearse por baja memoria");
                } else {
                    blockedList.appendLast(process);
                    logger.logEvent("BLOQUEADO: Proceso " + process.getNombre() + " (ID: " + process.getId() + 
                                  ") bloqueado por operación I/O");
                }
                break;
                
            case "Listo":
                checkMemoryAndSuspend();
                
                if (!memoryManager.canAllocate(process.getMemoriaRequerida()) || 
                    memoryManager.getAvailableMemory() < LOW_MEMORY_THRESHOLD) {
                    process.suspender();
                    process.setInMemory(false);
                    suspendedReadyList.appendLast(process);
                    memoryManager.deallocate(process.getMemoriaRequerida());
                    logger.logEvent("SUSPENDIDO: Proceso " + process.getNombre() + " (ID: " + process.getId() + 
                                  ") suspendido al pasar a listo por baja memoria");
                } else {
                    readyList.appendLast(process);
                }
                break;
                
            case "Suspendido-Listo":
                suspendedReadyList.appendLast(process);
                process.setInMemory(false);
                memoryManager.deallocate(process.getMemoriaRequerida());
                logger.logEvent("SUSPENDIDO: Proceso " + process.getNombre() + " (ID: " + process.getId() + 
                              ") suspendido (Listo)");
                break;
                
            case "Suspendido-Bloqueado":
                suspendedBlockedList.appendLast(process);
                process.setInMemory(false);
                memoryManager.deallocate(process.getMemoriaRequerida());
                logger.logEvent("SUSPENDIDO: Proceso " + process.getNombre() + " (ID: " + process.getId() + 
                              ") suspendido (Bloqueado)");
                break;
                
            case "Terminado":
                exitList.appendLast(process);
                if (process.isInMemory()) {
                    memoryManager.deallocate(process.getMemoriaRequerida());
                    process.setInMemory(false);
                }
                logger.logEvent("TERMINADO: Proceso " + process.getNombre() + " (ID: " + process.getId() + 
                              "). Memoria liberada: " + process.getMemoriaRequerida() + " MB");
                break;
                
            default:
                exitList.appendLast(process);
                if (process.isInMemory()) {
                    memoryManager.deallocate(process.getMemoriaRequerida());
                    process.setInMemory(false);
                }
                break;
        }

        updateAllLists();
    }

    public void updateAllLists() {
        updateReadyList();
        updateBlockedList();
        updateSuspendedLists();
        updateexitList();
        updateProcessList();
    }
    
    private void updateSuspendedLists() {
        StringBuilder displayReady = new StringBuilder();
        StringBuilder displayBlocked = new StringBuilder();

        Nodo pAux = suspendedReadyList.getHead();
        while (pAux != null) {
            Proceso p = (Proceso) pAux.getValue();
            displayReady.append("\n ----------------------------------\n ")
                .append("ID: ").append(p.getId())
                .append("\n Nombre: ").append(p.getNombre())
                .append("\n WT: ").append(p.getTiempoEspera())
                .append("\n Memoria: ").append(p.getMemoriaRequerida()).append(" MB");
            pAux = pAux.getpNext();
        }

        pAux = suspendedBlockedList.getHead();
        while (pAux != null) {
            Proceso p = (Proceso) pAux.getValue();
            displayBlocked.append("\n ----------------------------------\n ")
                .append("ID: ").append(p.getId())
                .append("\n Nombre: ").append(p.getNombre())
                .append("\n Memoria: ").append(p.getMemoriaRequerida()).append(" MB");
            pAux = pAux.getpNext();
        }

        controlador.setListosSuspendidosText(displayReady.toString());
        controlador.setBloqueadosSuspendidosText(displayBlocked.toString());
    }

    public void updateWaitingTime(){
        if(selectedAlgorithm != controlador.getPolitica()){
            selectedAlgorithm = controlador.getPolitica();
            sortReadyQueue(selectedAlgorithm);
            this.updateReadyList();
        }
        
        Nodo pAux = this.readyList.getHead();
        while(pAux!=null){
            Proceso process = (Proceso)pAux.getValue();
            int time = process.getTiempoEspera();
            process.setTiempoEspera(time+1);
            pAux = pAux.getpNext();
        }
        
        pAux = this.suspendedReadyList.getHead();
        while(pAux!=null){
            Proceso process = (Proceso)pAux.getValue();
            int time = process.getTiempoEspera();
            process.setTiempoEspera(time+1);
            pAux = pAux.getpNext();
        }
        
        this.updateProcessList();
    }
    
    public void updateBlockToReady(int id){
        Nodo pAux = this.blockedList.getHead();
        while(pAux!=null){
            if(id== ((Proceso)pAux.getValue()).getId()){
                Proceso p = (Proceso)pAux.getValue();
                p.setEstado("Listo");
                p.setTiempoEspera(0);
                blockedList.delete(pAux);
                
                checkMemoryAndSuspend();
                
                if (!memoryManager.canAllocate(p.getMemoriaRequerida()) || 
                    memoryManager.getAvailableMemory() < LOW_MEMORY_THRESHOLD) {
                    p.suspender();
                    p.setInMemory(false);
                    suspendedReadyList.appendLast(pAux);
                    memoryManager.deallocate(p.getMemoriaRequerida());
                    logger.logEvent("SUSPENDIDO: Proceso (ID: " + id + ") I/O completado pero suspendido por baja memoria");
                } else {
                    readyList.appendLast(pAux);
                    logger.logEvent("DESBLOQUEADO: Proceso (ID: " + id + ") I/O completado, movido a cola de listos");
                }
                break;                
            }
            pAux = pAux.getpNext();
        }
        
        pAux = this.suspendedBlockedList.getHead();
        while(pAux!=null){
            if(id== ((Proceso)pAux.getValue()).getId()){
                Proceso p = (Proceso)pAux.getValue();
                p.setEstado("Suspendido-Listo");
                p.setTiempoEspera(0);
                suspendedBlockedList.delete(pAux);
                suspendedReadyList.appendLast(pAux);
                logger.logEvent("I/O COMPLETADO: Proceso (ID: " + id + ") I/O completado, movido de Suspendido-Bloqueado a Suspendido-Listo");
                break;                
            }
            pAux = pAux.getpNext();
        }
        
        this.updateBlockedList();
        this.updateReadyList();
        this.updateSuspendedLists();
        this.updateProcessList();
    }
    
    public void updateProcessList(){
        Nodo pAux = allProcessList.getHead();
        String display = "";
        while(pAux!=null){
            Proceso process=(Proceso) pAux.getValue();
            display += this.stringInterfaz(process);
            pAux = pAux.getpNext();
        }
        controlador.setPcbs(display);
    }
    
    public void updateReadyList(){
        Nodo pAux = readyList.getHead();
        String display = "";
        while(pAux!=null){
            Proceso process=(Proceso) pAux.getValue();
            
            display += "\n ----------------------------------\n "
                    + "ID: " + process.getId() +
                      "\n Nombre: " + process.getNombre() +
                      "\n WT: " + process.getTiempoEspera();
            pAux = pAux.getpNext();
        }
        controlador.setListosText(display);
    }

    public void updateBlockedList(){
        Nodo pAux = blockedList.getHead();
        String display = "";
        while(pAux!=null){
            Proceso process=(Proceso) pAux.getValue();
            
            display += "\n ----------------------------------\n "
                    + "ID: " + process.getId() +
                      "\n Nombre: " + process.getNombre();
            pAux = pAux.getpNext();
        }
        controlador.setBloqueadosText(display);
    }
    
    public void updateexitList(){
        Nodo pAux = exitList.getHead();
        String display = "";
        while(pAux!=null){
            Proceso process=(Proceso) pAux.getValue();
            
            display += "\n ----------------------------------\n "
                    + "Id: " + process.getId() +
                      "\n Nombre: " + process.getNombre();
            pAux = pAux.getpNext();
        }
        controlador.setSalidaText(display);
    }
    
    public String stringInterfaz(Proceso currentProcess){
        String display = "\n ----------------------------------\n Id: " + currentProcess.getId() + 
                "\n Estado: " + currentProcess.getEstado()+ 
                "\n Nombre: " + currentProcess.getNombre() +
                "\n PC: " + currentProcess.getPc() + 
                "\n MAR: " + currentProcess.getMar() +
                "\n RT: " + (currentProcess.getInstrucciones() - currentProcess.getMar()) +
                "\n Espera: " + currentProcess.getTiempoEspera() +
                "\n Prioridad: " + currentProcess.getPrioridad() +
                "\n Memoria: " + (currentProcess.isInMemory() ? 
                    "En memoria (" + currentProcess.getMemoriaRequerida() + " MB)" : 
                    "Suspendido (" + currentProcess.getMemoriaRequerida() + " MB)");
        return display;
    }
}