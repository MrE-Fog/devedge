package c8y.pi4.agent.core.components;

import com.cumulocity.model.ID;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectReferenceRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.inventory.ManagedObject;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Ivelin Yanev
 * @since 02.09.2020
 *
 */
@Slf4j
public class DeviceManagedObject {
	private final IdentityApi registry;
	private final InventoryApi inventory;
	public static final String DEVICE_GROUP_TYPE = "c8y_DeviceGroup";

	public DeviceManagedObject(Platform platform) {
		this.registry = platform.getIdentityApi();
		this.inventory = platform.getInventoryApi();
	}

	public GId createOrUpdate(ManagedObjectRepresentation mo, ID extId, GId parentId) {
		GId gid = tryGetBinding(extId);

		ManagedObjectRepresentation returnedMo;
		returnedMo = (gid == null) ? create(extId, mo) : update(gid, mo);

		if (parentId != null) {
			ManagedObjectRepresentation handle = new ManagedObjectRepresentation();
			handle.setId(returnedMo.getId());
			handle.setSelf(returnedMo.getSelf());
			ManagedObjectReferenceRepresentation moRef = new ManagedObjectReferenceRepresentation();
			moRef.setManagedObject(handle);
			inventory.getManagedObjectApi(parentId).addChildDevice(moRef);
		}

		copyProps(returnedMo, mo);

		return returnedMo.getId();

	}

	private void copyProps(final ManagedObjectRepresentation returnedMo, final ManagedObjectRepresentation mo) {
		mo.setId(returnedMo.getId());
		mo.setName(returnedMo.getName());
		mo.setSelf(returnedMo.getSelf());
		mo.setAttrs(returnedMo.getAttrs());
	}

	/**
	 * @param gid
	 * @param mo
	 * @return
	 */
	private ManagedObjectRepresentation update(GId gid, ManagedObjectRepresentation mo) {
		mo.setId(gid);
		return inventory.update(mo);
	}

	/**
	 * @param extId
	 * @param mo
	 * @return
	 */
	private ManagedObjectRepresentation create(ID extId, ManagedObjectRepresentation mo) {
		ManagedObjectRepresentation returnedMo;
		returnedMo = inventory.create(mo);
		bind(returnedMo, extId);
		return returnedMo;
	}

	/**
	 * @param returnedMo
	 * @param extId
	 */
	private void bind(ManagedObjectRepresentation returnedMo, ID extId) {
		ExternalIDRepresentation eir = new ExternalIDRepresentation();
		eir.setExternalId(extId.getValue());
		eir.setType(extId.getType());
		eir.setManagedObject(returnedMo);
		registry.create(eir);

	}

	/**
	 * @param extId
	 * @return
	 */
	public GId tryGetBinding(ID extId) {
		ExternalIDRepresentation eir = null;
		try {
			eir = registry.getExternalId(extId);
		} catch (SDKException x) {
			if (x.getHttpStatus() != 404) {
				throw x;
			}
		}
		return eir != null ? eir.getManagedObject().getId() : null;
	}

	public GId createGroup(String name) {
		ManagedObjectRepresentation managedObject = new ManagedObjectRepresentation();

		final ID extID = new ID(name, "group");
		managedObject.setName(name);
		managedObject.setProperty("c8y_IsDeviceGroup", "{}");
		managedObject.setType(DEVICE_GROUP_TYPE);

		GId moID = createOrUpdate(managedObject, extID, null);
		return moID;
	}

	public void assignToGroup(GId objectId, GId groupId) {
		ManagedObject managedObjectApi = inventory.getManagedObjectApi(groupId);
		managedObjectApi.addChildAssets(objectId);

	}
}
