package modelos;

import micelaneos.*;

public class MemoryManager {
    private int totalMemory;
    private int availableMemory;
    private static final int DEFAULT_MEMORY = 500;

    public MemoryManager() {
        this.totalMemory = DEFAULT_MEMORY;
        this.availableMemory = DEFAULT_MEMORY;
    }

    public MemoryManager(int totalMemory) {
        this.totalMemory = totalMemory;
        this.availableMemory = totalMemory;
    }

    public synchronized boolean canAllocate(int memoryNeeded) {
        return availableMemory >= memoryNeeded;
    }

    public synchronized boolean allocate(int memoryAmount) {
        if (canAllocate(memoryAmount)) {
            availableMemory -= memoryAmount;
            return true;
        }
        return false;
    }

    public synchronized void deallocate(int memoryAmount) {
        availableMemory += memoryAmount;
        if (availableMemory > totalMemory) {
            availableMemory = totalMemory;
        }
    }

    public synchronized int getAvailableMemory() {
        return availableMemory;
    }

    public synchronized int getTotalMemory() {
        return totalMemory;
    }

    public synchronized double getMemoryUtilization() {
        return ((double)(totalMemory - availableMemory) / totalMemory) * 100;
    }
}