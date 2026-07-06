import client from "./client";

export const getVehicleCatalog = () => client.get("/vehicle-catalog");
