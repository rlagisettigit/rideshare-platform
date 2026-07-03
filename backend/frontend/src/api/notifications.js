import client from "./client";

export const getMyNotifications = () => client.get("/notifications/me");
