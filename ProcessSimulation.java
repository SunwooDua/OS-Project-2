import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class ProcessSimulation {
    public static void main(String[] args) {
        // Step 1: Read processes from file
        List<Process> processes = readProcessesFromFile("processes.txt");
        if (processes.isEmpty()) {
            System.out.println("No processes found in the input file.");
            return;
        }
        
        System.out.println("Starting process simulation with " + processes.size() + " processes...\n");
        
        // Step 2: Create and start process threads
        List<ProcessThread> processThreads = new ArrayList<>();
        for (Process p : processes) {
            ProcessThread thread = new ProcessThread(p.getPid(), p.getBurstTime());
            processThreads.add(thread);
            thread.start();
        }
        
        // Wait for all process threads to complete
        for (ProcessThread thread : processThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("Thread was interrupted: " + e.getMessage());
            }
        }
        
        System.out.println("\nAll processes have completed execution.");
        
        // Step 3: Demonstrate a synchronization problem (Producer-Consumer)
        System.out.println("\n*** Producer-Consumer Problem Demonstration ***\n");
        
        // Create a bounded buffer with capacity of 5
        BoundedBuffer buffer = new BoundedBuffer(5);
        
        // Create producer and consumer threads
        Producer producer = new Producer(buffer, 10); // Produce 10 items
        Consumer consumer1 = new Consumer(buffer, "Consumer-1", 5); // Consume 5 items
        Consumer consumer2 = new Consumer(buffer, "Consumer-2", 5); // Consume 5 items
        
        // Start the threads
        producer.start();
        consumer1.start();
        consumer2.start();
        
        // Wait for all threads to complete
        try {
            producer.join();
            consumer1.join();
            consumer2.join();
        } catch (InterruptedException e) {
            System.out.println("Thread was interrupted: " + e.getMessage());
        }
        
        System.out.println("\nProducer-Consumer demonstration completed.");
    }
    
    /**
     * Reads process information from a file
     * @param filename The name of the file to read from
     * @return A list of Process objects
     */
    private static List<Process> readProcessesFromFile(String filename) {
        List<Process> processes = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines or comments
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }
                
                // Parse the line (expected format: pid,burstTime)
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    int pid = Integer.parseInt(parts[0].trim());
                    int burstTime = Integer.parseInt(parts[1].trim());
                    processes.add(new Process(pid, burstTime));
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            // Create some sample processes if file cannot be read
            System.out.println("Using sample process data instead.");
            processes.add(new Process(1, 2));
            processes.add(new Process(2, 1));
            processes.add(new Process(3, 3));
        } catch (NumberFormatException e) {
            System.out.println("Error parsing numeric values: " + e.getMessage());
        }
        
        return processes;
    }
}

/**
 * Process class to store process information
 */
class Process {
    private int pid;
    private int burstTime;
    
    public Process(int pid, int burstTime) {
        this.pid = pid;
        this.burstTime = burstTime;
    }
    
    public int getPid() {
        return pid;
    }
    
    public int getBurstTime() {
        return burstTime;
    }
}

/**
 * Thread class to simulate process execution
 */
class ProcessThread extends Thread {
    private int pid;
    private int burstTime;
    
    public ProcessThread(int pid, int burstTime) {
        this.pid = pid;
        this.burstTime = burstTime;
    }
    
    @Override
    public void run() {
        System.out.println("[Process " + pid + "] Started. Burst time: " + burstTime + "s");
        try {
            // Simulate CPU burst
            Thread.sleep(burstTime * 1000);
        } catch (InterruptedException e) {
            System.out.println("[Process " + pid + "] Interrupted");
        }
        System.out.println("[Process " + pid + "] Finished execution after " + burstTime + "s");
    }
}

/**
 * BoundedBuffer class that implements the shared buffer for Producer-Consumer
 */
class BoundedBuffer {
    private final int capacity;
    private final Queue<Integer> buffer;
    private final Semaphore mutex;       // Controls access to critical section
    private final Semaphore empty;       // Counts empty buffer slots
    private final Semaphore full;        // Counts filled buffer slots
    
    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new LinkedList<>();
        this.mutex = new Semaphore(1);   // Binary semaphore (mutex)
        this.empty = new Semaphore(capacity);
        this.full = new Semaphore(0);
    }
    
    /**
     * Adds an item to the buffer (called by producer)
     */
    public void produce(int item) throws InterruptedException {
        System.out.println("[Producer] Waiting to add item: " + item);
        
        // Wait for an empty slot
        empty.acquire();
        
        // Enter critical section
        mutex.acquire();
        
        // Add item to buffer
        buffer.add(item);
        System.out.println("[Producer] Added item: " + item + " (Buffer size: " + buffer.size() + "/" + capacity + ")");
        
        // Exit critical section
        mutex.release();
        
        // Signal that a new item is in the buffer
        full.release();
    }
    
    /**
     * Removes an item from the buffer (called by consumer)
     */
    public int consume(String consumerName) throws InterruptedException {
        System.out.println("[" + consumerName + "] Waiting to consume an item");
        
        // Wait for an item to be available
        full.acquire();
        
        // Enter critical section
        mutex.acquire();
        
        // Remove item from buffer
        int item = buffer.poll();
        System.out.println("[" + consumerName + "] Consumed item: " + item + " (Buffer size: " + buffer.size() + "/" + capacity + ")");
        
        // Exit critical section
        mutex.release();
        
        // Signal that a slot is now empty
        empty.release();
        
        return item;
    }
}

/**
 * Producer thread that adds items to the buffer
 */
class Producer extends Thread {
    private final BoundedBuffer buffer;
    private final int itemCount;
    
    public Producer(BoundedBuffer buffer, int itemCount) {
        this.buffer = buffer;
        this.itemCount = itemCount;
        setName("Producer");
    }
    
    @Override
    public void run() {
        try {
            for (int i = 1; i <= itemCount; i++) {
                // Simulate some time to produce an item
                Thread.sleep((int)(Math.random() * 1000));
                
                // Add the item to buffer
                buffer.produce(i);
            }
            System.out.println("[Producer] Finished producing all items");
        } catch (InterruptedException e) {
            System.out.println("[Producer] Interrupted: " + e.getMessage());
        }
    }
}

/**
 * Consumer thread that removes items from the buffer
 */
class Consumer extends Thread {
    private final BoundedBuffer buffer;
    private final int itemCount;
    
    public Consumer(BoundedBuffer buffer, String name, int itemCount) {
        this.buffer = buffer;
        this.itemCount = itemCount;
        setName(name);
    }
    
    @Override
    public void run() {
        try {
            for (int i = 0; i < itemCount; i++) {
                // Simulate some time to consume an item
                Thread.sleep((int)(Math.random() * 1500));
                
                // Consume an item from buffer
                buffer.consume(getName());
            }
            System.out.println("[" + getName() + "] Finished consuming all items");
        } catch (InterruptedException e) {
            System.out.println("[" + getName() + "] Interrupted: " + e.getMessage());
        }
    }
}
