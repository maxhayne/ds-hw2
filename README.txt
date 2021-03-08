Student: Max Hayne
Assignment: 2

Notes to TA:
My scripts should be callable just as they are required to be in the assignment description. Please note that the 'batch-time' input for the Server, I'm assuming will be given in milliseconds, not seconds. Both the client and receivers should be cancellable using CTRL-C. Also, neither the Server or the Client take commands while running.


----- OVERVIEW OF FILES -----

CLIENT FOLDER:

'Client.java' is the base code for my client. The sender, receiver, traffic reporter are spawned from within the client.

'ClientReceiver.java' is the class that holds code for the receiver. It extends Thread, and inside the run method data is read from the socketchannel representing the connection to the server.

'ClientSender.java' is the class that holds code for the sender. It extends TimerTask, and so the run method is called four times per second (or however often you decide to send messages to the server).

SERVER FOLDER:

'Server.java' is what you'd expect it to be - code that launches the thread pool manager, and the statistics reporter.

'PoolThread.java' extends Thread and is the class representing every thread in the pool. Each thread in the thread pool continuously checks for batches that have been added to a LinkedBlockingQueue by the thread pool manager. Each thread can also add to a LinkedBlockingQueue of tasks that the thread pool manager will pull from to create batches.

'ThreadPoolManager.java' extends Thread and is the class representing the thread pool manager (TPM). It maintins two LinkedBlockingQueues, one for batches, and one for tasks. The TPM is the only object with access to the batchQueue, while all threads in the thread pool also have access to the taskQueue. A selector is looped upon to retreive selection keys that have data to be read (or accepted), and new readTasks (or registrationTasks) are created containing those selection keys and added to the taskQueue. When either the taskQueue has reached batchSize or the batchTime has expired, tasks are taken from the taskQueue, placed into an ArrayList together, and that list is added to the batchQueue.

TASKS FOLDER:

'HashCodeBatchTask.java' is the class representing the task that has to do with generating a hash based on an 8KB byte array. It returns a WriteBatchTask.

'ReadBatchTask.java' is the class representing the task that reads data from the socket channel pointed to by a selection key. If successful, it returns a HashCodeBatchTask.

'RegisterBatchTask.java' is the class representing the task of registering new clients. It takes in the selection key of the serversocket, and calls an accept on that serversocket, and registers the resulting socketchannel with the selector. It returns null.

'WriteBatchTask.java' is the class representing the task of writing a hash code to a client's socketchannel. If a thread in the pool gets a WriteBatchTask to execute, a write attempt is made, and if another thread is currently writing to the same socket, the task returns itself, and the thread pool adds that write task to the taskQueue once again for another attempt.

'Task.java' is the interface which every task implements. It only contains one method: doTask(), which is allows every pool thread to be blind to the task they are doing at any given time.

UTIL FOLDER:

'ClientStatisticsReporter.java' is exactly what it sounds like. It extends TimerTask, and is called once every twenty seconds. It contains atomic variables that keep track of send and receive counts for the client.

'StatisticLock.java' is a custom object I created to attach to every SelectionKey upon registration of a new client. It holds an atomic counter for processed work done for that client, and a semaphore that is only given out the worker thread currently writing to that client's socketchannel. This solves the issue of concurrent writes. It can be iterated over by the ServerStatisticsReporter to collect information about processed work.

'ServerStatisticsReporter.java' is also exactly what it sounds like. It extends TimerTask and is called once every twenty seconds. What is different here, is that it has a synchronized block on the keyset returned by the Selector.keys() command, so that when it is iterating through all keys being maintined by the selector in order to extract meaningful data from every key's attached StatisticLock object, the select() call in the TPM blocks. This ensures that no ConcurrentModificationExceptions are thrown. When it receives all the stats it needs, it computes means and standard deviations, and prints to the terminal.
