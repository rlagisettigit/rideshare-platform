import client from "./client";

export const getPendingReviews = () => client.get("/reviews/pending");
export const getReceivedReviews = () => client.get("/reviews/received");
export const submitReview = (payload) => client.post("/reviews", payload);
