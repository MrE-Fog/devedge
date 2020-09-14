package c8y.devteams.agent.driver.devices;

import java.math.BigDecimal;

import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;

import c8y.Hardware;
import c8y.TemperatureMeasurement;
import c8y.TemperatureSensor;
import c8y.devteams.agent.driver.MeasurementPollingDriver;
import c8y.devteams.agent.driver.impl.DeviceManager;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Ivelin Yanev
 * @since 11.09.2020
 *
 */
@Slf4j
public abstract class AbstractTemperatureSensor extends MeasurementPollingDriver {

	private static final String TYPE = "Temperature";

	private final String id;

	/**
	 * 
	 * @param id
	 */
	public AbstractTemperatureSensor(String id) {
		super("c8y_" + TYPE + "Sensor", 5000);

		this.id = id;
	}

	@Override
	public void initialize() throws Exception {
		log.info("initializing");
	}

	@Override
	public void run() {
		double temperature = getTemperature();

		TemperatureMeasurement temperatureMeasurement = new TemperatureMeasurement();
		temperatureMeasurement.setTemperature(new BigDecimal(temperature));

		sendMeasurement(temperatureMeasurement);

		log.info("sending temperature measurement: " + temperature);
	}

	@Override
	public void discoverChildren(ManagedObjectRepresentation parent) {
		log.info("creating child");

		ManagedObjectRepresentation childDevice = DeviceManager.createChild(id, TYPE, getPlatform(), parent,
				getHardware(), getSupportedOperations(), new TemperatureSensor());
		
		 setSource(childDevice);

	}

	public abstract Hardware getHardware();

	public abstract double getTemperature();

}
