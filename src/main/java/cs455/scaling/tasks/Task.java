package cs455.scaling.tasks;
import java.io.IOException;

public interface Task {

	// Worker threads need not know
	// the type of task being done
	
	Task doTask() throws IOException;
}