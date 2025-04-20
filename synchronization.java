class ProcessThread extends Thread {
    int pid, burstTime;
    public ProcessThread(int pid, int burstTime) {
    this.pid = pid;
    this.burstTime = burstTime;
    }
    public void run() {
    System.out.println("Process " + pid + " started.");
    try {
    Thread.sleep(burstTime * 1000);
    } catch (InterruptedException e) {}
    System.out.println("Process " + pid + " finished.");
}
}