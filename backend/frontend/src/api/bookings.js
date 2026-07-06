import client from "./client";

export const createBooking = (payload) => client.post("/bookings", payload);
export const getMyBookings = () => client.get("/bookings/me");
export const getBooking = (publicId) => client.get(`/bookings/${publicId}`);
export const getDriverBookingRequests = () => client.get("/bookings/driver");
export const acceptBooking = (publicId) => client.post(`/bookings/${publicId}/accept`);
export const rejectBooking = (publicId) => client.post(`/bookings/${publicId}/reject`);
export const cancelBooking = (publicId, reason) => client.post(`/bookings/${publicId}/cancel`, { reason });
export const acceptBookingBatch = (batchId) => client.post(`/bookings/batch/${batchId}/accept`);
export const rejectBookingBatch = (batchId) => client.post(`/bookings/batch/${batchId}/reject`);
