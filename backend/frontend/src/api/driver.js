import client from "./client";

export const onboardDriver = (payload) => client.post("/drivers/onboard", payload);
export const goOnline = () => client.post("/drivers/availability/online");
export const goOffline = () => client.post("/drivers/availability/offline");
export const registerVehicle = (payload) => client.post("/vehicles", payload);
export const getMyVehicles = () => client.get("/vehicles/me");
export const updateVehicle = (vehicleId, payload) => client.put(`/vehicles/${vehicleId}`, payload);
export const deleteVehicle = (vehicleId) => client.delete(`/vehicles/${vehicleId}`);
