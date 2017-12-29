package lse.neko.demo.sanders;

import org.apache.java.util.Configurations;

import lse.neko.LayerInterface;
import lse.neko.NekoProcess;
import lse.neko.NekoProcessInitializer;
import lse.neko.layers.NoMulticastLayer;

public class Initializer implements NekoProcessInitializer {

	public void init(NekoProcess process, Configurations config) throws Exception {
		
		// Cria as classes para as pilhas de protocolo
		Class monitorAlgorithmClass =
            Class.forName(config.getString("monitor"));
        Class applicationAlgorithmClass =
            Class.forName(config.getString("application"));
        
        Class[] constructorParamClasses = { NekoProcess.class };
        Object[] constructorParams = { process };
        LayerInterface monitor = (LayerInterface)
            monitorAlgorithmClass.getConstructor(constructorParamClasses).newInstance(constructorParams);
        LayerInterface application = (LayerInterface)
            applicationAlgorithmClass.getConstructor(constructorParamClasses).newInstance(constructorParams);

        // Instancia as pilhas do protocolo
        process.addLayer(new NoMulticastLayer(process));
        process.addLayer(monitor);
	    process.addLayer(application);
		
	}

	
}
