import client from "./client";

export const getMyProfile = () => client.get("/users/me");
export const updateMyProfile = (payload) => client.patch("/users/me", payload);
