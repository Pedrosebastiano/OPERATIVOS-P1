package modelos;

import micelaneos.*;

public class PerformanceMetrics {
    private int totalCpuTime;
    private int totalSystemTime;
    private int processesCompleted;
    private double totalWaitTime;
    private double totalResponseTime;
    private int totalCycles;

    public PerformanceMetrics() {
        this.totalCpuTime = 0;
        this.totalSystemTime = 0;
        this.processesCompleted = 0;
        this.totalWaitTime = 0;
        this.totalResponseTime = 0;
        this.totalCycles = 0;
    }

    public synchronized void incrementCpuTime() {
        this.totalCpuTime++;
        this.totalCycles++;
    }

    public synchronized void incrementSystemTime() {
        this.totalSystemTime++;
        this.totalCycles++;
    }

    public synchronized void recordProcessCompletion(Proceso proceso) {
        this.processesCompleted++;
        this.totalWaitTime += proceso.getTiempoEspera();
        this.totalResponseTime += proceso.getTiempoRespuesta();
    }

    public double getThroughput() {
        if (totalCycles == 0) return 0;
        return (double) processesCompleted / totalCycles;
    }

    public double getCpuUtilization() {
        if (totalCycles == 0) return 0;
        return (double) totalCpuTime / totalCycles * 100;
    }

    public double getAverageWaitTime() {
        if (processesCompleted == 0) return 0;
        return totalWaitTime / processesCompleted;
    }

    public double getAverageResponseTime() {
        if (processesCompleted == 0) return 0;
        return totalResponseTime / processesCompleted;
    }

    public double getFairness(List allProcesses) {
        if (allProcesses.getSize() == 0) return 1.0;
        
        double sumWaitTimes = 0;
        double sumSquaredDiff = 0;
        int count = 0;
        
        Nodo current = allProcesses.getHead();
        while (current != null) {
            Proceso p = (Proceso) current.getValue();
            sumWaitTimes += p.getTiempoEspera();
            count++;
            current = current.getpNext();
        }
        
        if (count == 0) return 1.0;
        double avgWaitTime = sumWaitTimes / count;
        
        current = allProcesses.getHead();
        while (current != null) {
            Proceso p = (Proceso) current.getValue();
            double diff = p.getTiempoEspera() - avgWaitTime;
            sumSquaredDiff += diff * diff;
            current = current.getpNext();
        }
        
        double variance = sumSquaredDiff / count;
        double stdDev = Math.sqrt(variance);
        
        if (avgWaitTime == 0) return 1.0;
        double cv = stdDev / avgWaitTime;
        return 1.0 / (1.0 + cv);
    }

    public int getProcessesCompleted() {
        return processesCompleted;
    }

    public int getTotalCycles() {
        return totalCycles;
    }

    public String getMetricsString() {
        return String.format(
            "Throughput: %.4f procesos/ciclo\n" +
            "Utilización CPU: %.2f%%\n" +
            "Tiempo Espera Promedio: %.2f ciclos\n" +
            "Tiempo Respuesta Promedio: %.2f ciclos\n" +
            "Procesos Completados: %d\n" +
            "Ciclos Totales: %d",
            getThroughput(),
            getCpuUtilization(),
            getAverageWaitTime(),
            getAverageResponseTime(),
            processesCompleted,
            totalCycles
        );
    }
}