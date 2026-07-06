import client from "./client";

export const postLocation = (ridePublicId, { lat, lng, heading }) =>
  client.post(`/rides/${ridePublicId}/location`, { lat, lng, heading });

export const getLocation = (ridePublicId) => client.get(`/rides/${ridePublicId}/location`);
export const getRoute = (ridePublicId) => client.get(`/rides/${ridePublicId}/route`);
