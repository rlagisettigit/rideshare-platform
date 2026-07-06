import client from "./client";

export const createRecurringRide = (payload) => client.post("/recurring-rides", payload);
export const getMyRecurringRides = () => client.get("/recurring-rides/me");
export const cancelRecurringRide = (publicId) => client.post(`/recurring-rides/${publicId}/cancel`);
export const bookAllRecurring = (publicId, payload) => client.post(`/recurring-rides/${publicId}/book`, payload);
export const getRecurringOccurrences = (publicId) => client.get(`/recurring-rides/${publicId}/occurrences`);
