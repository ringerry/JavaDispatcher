package Ru.IVT.JWT_REST_Dispatcher;

import Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.Impl.DispatcherEngineImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JwtDispatcherApplication {

	private static DispatcherEngineImpl dispatcherEngine;

	public static void main(String[] args) {

		dispatcherEngine = new DispatcherEngineImpl();

		SpringApplication.run(JwtDispatcherApplication.class, args);
	}

	public static DispatcherEngineImpl getDispatcherEngine() {
		return dispatcherEngine;
	}
}
