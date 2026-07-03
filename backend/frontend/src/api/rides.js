import client from "./client";

export const publishRide = (payload) => client.post("/rides", payload);
export const getMyRides = () => client.get("/rides/me");
export const getRide = (publicId) => client.get(`/rides/${publicId}`);
export const cancelRide = (publicId) => client.post(`/rides/${publicId}/cancel`);
export const startRide = (publicId) => client.post(`/rides/${publicId}/start`);
export const finishRide = (publicId) => client.post(`/rides/${publicId}/finish`);
export const searchRides = (payload) => client.post("/search/rides", payload);
