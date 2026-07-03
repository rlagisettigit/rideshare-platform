import client from "./client";

export const getMyPayments = () => client.get("/payments/me");
