package org.chris.portmapper.router;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sbbi.upnp.devices.UPNPRootDevice;
import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.ActionResponse;
import net.sbbi.upnp.messages.UPNPResponseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Router {

	private Log logger = LogFactory.getLog(this.getClass());
	private InternetGatewayDevice router = null;
	private final static int DISCOVERY_TIMEOUT = 5;

	private Router(InternetGatewayDevice router) {
		if (router == null) {
			throw new IllegalArgumentException("No router given");
		}
		this.router = router;
	}

	public static Router findRouter() throws RouterException {
		InternetGatewayDevice devices = findInternetGatewayDevice();
		Router r = new Router(devices);
		return r;
	}

	private static InternetGatewayDevice findInternetGatewayDevice()
			throws RouterException {
		InternetGatewayDevice[] devices;
		try {
			devices = InternetGatewayDevice.getDevices(DISCOVERY_TIMEOUT);
		} catch (IOException e) {
			throw new RouterException("Could not find devices", e);
		}

		if (devices == null || devices.length == 0) {
			throw new RouterException("No router devices found");
		}

		if (devices.length != 1) {
			throw new RouterException("Found more than one router devices ("
					+ devices.length + ")");
		}
		return devices[0];
	}

	public String getName() throws RouterException {
		return router.getIGDRootDevice().getModelName();
	}

	public String getExternalIPAddress() throws RouterException {
		logger.info("Get external IP address...");
		String ipAddress;
		try {
			ipAddress = router.getExternalIPAddress();
		} catch (UPNPResponseException e) {
			throw new RouterException("Could not get external IP", e);
		} catch (IOException e) {
			throw new RouterException("Could not get external IP", e);
		}
		logger.info("Got external IP address " + ipAddress);
		return ipAddress;
	}

	public String getInternalIPAddress() {
		logger.info("Get internal IP address...");
		String ipAddress;
		ipAddress = router.getIGDRootDevice().getPresentationURL().getHost();
		logger.info("Got internal IP address " + ipAddress);
		return ipAddress;
	}

	public Collection<PortMapping> getPortMappings() throws RouterException {
		logger.info("Get all port mappings...");
		Collection<PortMapping> mappings = new LinkedList<PortMapping>();
		try {

			boolean moreEntries = true;
			int i = 0;
			while (moreEntries) {
				logger.debug("Get port mapping entry number " + i);
				ActionResponse response = null;
				try {
					response = router.getGenericPortMappingEntry(i);
				} catch (UPNPResponseException e) {
					if (e
							.getMessage()
							.equals(
									"Detailed error code :713, Detailed error description :SpecifiedArrayIndexInvalid")) {
						moreEntries = false;
						logger.debug("Got no more port mappings");
					} else {
						throw e;
					}
				}
				if (response != null) {
					mappings.add(PortMapping.create(response));
				}
				i++;
			}

		} catch (IOException e) {
			throw new RouterException("Could not get NAT mappings", e);
		} catch (UPNPResponseException e) {
			throw new RouterException("Could not get NAT mappings", e);
		}
		return mappings;
	}

	public void logRouterInfo() throws RouterException {
		Map<String, String> info = new HashMap<String, String>();
		UPNPRootDevice rootDevice = router.getIGDRootDevice();
		info.put("friendlyName", rootDevice.getFriendlyName());
		info.put("manufacturer", rootDevice.getManufacturer());
		info.put("modelDescription", rootDevice.getModelDescription());
		info.put("modelName", rootDevice.getModelName());
		info.put("serialNumber", rootDevice.getSerialNumber());
		info.put("vendorFirmware", rootDevice.getVendorFirmware());

		info.put("modelNumber", rootDevice.getModelNumber());
		info.put("modelURL", rootDevice.getModelURL());
		info.put("manufacturerURL", rootDevice.getManufacturerURL()
				.toExternalForm());
		info.put("presentationURL", rootDevice.getPresentationURL()
				.toExternalForm());
		info.put("urlBase", rootDevice.getURLBase().toExternalForm());

		SortedSet<String> sortedKeys = new TreeSet<String>(info.keySet());

		for (String key : sortedKeys) {
			String value = info.get(key);
			logger.info("Router Info: " + key + " \t= " + value);
		}

		// for (Object service : rootDevice.getServices()) {
		// UPNPService upnpService = (UPNPService) service;
		// info = new HashMap<String, String>();
		// info.put("serviceID", upnpService.getServiceId());
		// info.put("serviceType", upnpService.getServiceType());
		// info.put("scpdURL", upnpService.getSCPDURL().toExternalForm());
		//
		// sortedKeys = new TreeSet<String>(info.keySet());
		//
		// for (String key : sortedKeys) {
		// String value = info.get(key);
		// logger.info("Router Service: " + key + " \t= " + value);
		// }
		//
		// logActions(upnpService);
		//
		// logStateVariables(upnpService);
		// }
	}

	// private void logStateVariables(UPNPService upnpService)
	// throws RouterException {
	// for (Iterator<?> iterator = upnpService.getAvailableStateVariableName();
	// iterator
	// .hasNext();) {
	// String stateVariableName = (String) iterator.next();
	// ServiceStateVariable stateVar = upnpService
	// .getUPNPServiceStateVariable(stateVariableName);
	// try {
	// logger.info("State Variable " + stateVar.getName()
	// + " value : " + stateVar.getValue() + " data type: "
	// + stateVar.getDataType());
	// } catch (UPNPResponseException e) {
	// throw new RouterException(
	// "Could not get info for state variable "
	// + stateVariableName, e);
	// } catch (IOException e) {
	// throw new RouterException(
	// "Could not get info for state variable "
	// + stateVariableName, e);
	// }
	// }
	// }
	//
	// private void logActions(UPNPService upnpService) {
	// for (Iterator<?> iterator = upnpService.getAvailableActionsName();
	// iterator
	// .hasNext();) {
	// String actionName = (String) iterator.next();
	// ServiceAction action = upnpService.getUPNPServiceAction(actionName);
	// logger.info("Action " + action.getName() + " output arguements: "
	// + action.getOutputActionArgumentsNames()
	// + " input arguments: "
	// + action.getInputActionArgumentsNames());
	// }
	// }

	private boolean addPortMapping(String description, Protocol protocol,
			String remoteHost, int externalPort, String internalClient,
			int internalPort, int leaseDuration) throws RouterException {
		String protocolString = (protocol.equals(Protocol.TCP) ? "TCP" : "UDP");
		try {
			boolean success = router.addPortMapping(description, null,
					internalPort, externalPort, internalClient, leaseDuration,
					protocolString);
			return success;
		} catch (IOException e) {
			throw new RouterException("Could not add port mapping", e);
		} catch (UPNPResponseException e) {
			throw new RouterException("Could not add port mapping", e);
		}
	}

	public boolean addPortMappings(Collection<PortMapping> mappings)
			throws RouterException {
		for (PortMapping portMapping : mappings) {
			logger.info("Adding port mapping " + portMapping);
			boolean success = addPortMapping(portMapping);
			if (!success) {
				return false;
			}
		}
		return true;
	}

	public boolean addPortMapping(PortMapping mapping) throws RouterException {
		logger.info("Adding port mapping " + mapping.getCompleteDescription());
		return addPortMapping(mapping.getDescription(), mapping.getProtocol(),
				mapping.getRemoteHost(), mapping.getExternalPort(), mapping
						.getInternalClient(), mapping.getInternalPort(), 0);
	}

	public boolean removeMapping(PortMapping mapping) throws RouterException {
		return removePortMapping(mapping.getProtocol(),
				mapping.getRemoteHost(), mapping.getExternalPort());

	}

	public boolean removePortMapping(Protocol protocol, String remoteHost,
			int externalPort) throws RouterException {
		String protocolString = (protocol.equals(Protocol.TCP) ? "TCP" : "UDP");
		try {
			boolean success = router.deletePortMapping(remoteHost,
					externalPort, protocolString);
			return success;
		} catch (IOException e) {
			throw new RouterException("Could not remove SSH port mapping", e);
		} catch (UPNPResponseException e) {
			throw new RouterException("Could not remove SSH port mapping", e);
		}
	}

	public void disconnect() {

	}

	public long getValidityTime() {
		return router.getIGDRootDevice().getValidityTime();
	}
}