package micelaneos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Proceso {
    private int id;
    private String nombre;
    private int instrucciones;
    private String tipo;
    private int ciclosParaExcepcion;
    private int ciclosParaSatisfacerExcepcion;
    private int pc; 
    private int mar; 
    private int prioridad;
    private String estado;
    private int tiempoEspera;
    private int tiempoRespuesta;
    private int tiempoInicio;
    private int tiempoFinalizacion;
    private boolean primerEjecucion;
    private int memoriaRequerida;
    private boolean inMemory; // NEW: Track if process is in memory

    public Proceso() {
        this.primerEjecucion = true;
        this.tiempoInicio = -1;
        this.tiempoFinalizacion = -1;
        this.memoriaRequerida = 0;
        this.inMemory = true; // NEW: Default to in memory
    }

    @JsonCreator
    public Proceso(@JsonProperty("id") int id,
                   @JsonProperty("nombre") String nombre,
                   @JsonProperty("tipo") String tipo,
                   @JsonProperty("instrucciones") int instrucciones,
                   @JsonProperty("ciclosParaExcepcion") int ciclosParaExcepcion,
                   @JsonProperty("ciclosParaSatisfacerExcepcion") int ciclosParaSatisfacerExcepcion,
                   @JsonProperty("prioridad") int prioridad) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.instrucciones = instrucciones;
        this.ciclosParaExcepcion = ciclosParaExcepcion;
        this.ciclosParaSatisfacerExcepcion = ciclosParaSatisfacerExcepcion;
        this.prioridad = prioridad;
        this.pc = 1;
        this.mar = 0;
        this.tiempoEspera = 1;
        this.estado = "Listo";
        this.primerEjecucion = true;
        this.tiempoInicio = -1;
        this.tiempoFinalizacion = -1;
        this.tiempoRespuesta = 0;
        this.memoriaRequerida = calcularMemoriaRequerida(instrucciones);
        this.inMemory = true; // NEW: Default to in memory
    }

    private int calcularMemoriaRequerida(int instrucciones) {
        // Similar to JuanGzlz's implementation: duration / 10
        return Math.max(10, instrucciones / 2);
    }

    // NEW: Memory management methods
    public int getMemoriaRequerida() {
        return memoriaRequerida;
    }

    public void setMemoriaRequerida(int memoriaRequerida) {
        this.memoriaRequerida = memoriaRequerida;
    }

    public boolean isInMemory() {
        return inMemory;
    }

    public void setInMemory(boolean inMemory) {
        this.inMemory = inMemory;
    }

    // NEW: Check if process should be suspended due to memory constraints
    public boolean debeSuspenderse(int memoriaDisponible) {
        return this.memoriaRequerida > memoriaDisponible;
    }

    // NEW: Suspend the process
    public void suspender() {
        if (this.estado.equals("Bloqueado")) {
            this.estado = "Suspendido-Bloqueado";
        } else if (this.estado.equals("Listo")) {
            this.estado = "Suspendido-Listo";
        }
        this.inMemory = false;
    }

    // NEW: Reactivate the process
    public void reactivar() {
        if (this.estado.equals("Suspendido-Listo")) {
            this.estado = "Listo";
        } else if (this.estado.equals("Suspendido-Bloqueado")) {
            this.estado = "Bloqueado";
        }
        this.inMemory = true;
    }

    // Existing getters and setters
    public int getTiempoRespuesta() {
        return tiempoRespuesta;
    }

    public void setTiempoRespuesta(int tiempoRespuesta) {
        this.tiempoRespuesta = tiempoRespuesta;
    }

    public int getTiempoInicio() {
        return tiempoInicio;
    }

    public void setTiempoInicio(int tiempoInicio) {
        this.tiempoInicio = tiempoInicio;
    }

    public int getTiempoFinalizacion() {
        return tiempoFinalizacion;
    }

    public void setTiempoFinalizacion(int tiempoFinalizacion) {
        this.tiempoFinalizacion = tiempoFinalizacion;
    }

    public boolean isPrimerEjecucion() {
        return primerEjecucion;
    }

    public void setPrimerEjecucion(boolean primerEjecucion) {
        this.primerEjecucion = primerEjecucion;
    }

    public int getTiempoEspera() {
        return tiempoEspera;
    }

    public void setTiempoEspera(int tiempoEspera) {
        this.tiempoEspera = tiempoEspera;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getInstrucciones() {
        return instrucciones;
    }

    public void setInstrucciones(int instrucciones) {
        this.instrucciones = instrucciones;
    }

    public int getCiclosParaExcepcion() {
        return ciclosParaExcepcion;
    }

    public void setCiclosParaExcepcion(int ciclosParaExcepcion) {
        this.ciclosParaExcepcion = ciclosParaExcepcion;
    }

    public int getCiclosParaSatisfacerExcepcion() {
        return ciclosParaSatisfacerExcepcion;
    }

    public void setCiclosParaSatisfacerExcepcion(int ciclosParaSatisfacerExcepcion) {
        this.ciclosParaSatisfacerExcepcion = ciclosParaSatisfacerExcepcion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int getMar() {
        return mar;
    }

    public void setMar(int mar) {
        this.mar = mar;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        if (estado.equals("Nuevo") ||
            estado.equals("Listo") ||
            estado.equals("Ejecucion") ||
            estado.equals("Bloqueado") ||
            estado.equals("Suspendido-Listo") ||
            estado.equals("Suspendido-Bloqueado") ||
            estado.equals("Terminado")) {
            this.estado = estado;
        } else {
            System.err.println("Estado no reconocido: " + estado);
        }
    }

    @Override
    public String toString() {
        return "Proceso{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", estado='" + estado + '\'' +
                ", instrucciones=" + instrucciones +
                ", memoria=" + memoriaRequerida + " MB" +
                ", inMemory=" + inMemory +
                '}';
    }
}