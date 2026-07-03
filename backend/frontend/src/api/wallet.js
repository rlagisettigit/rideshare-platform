import client from "./client";

export const getMyWallet = () => client.get("/wallet/me");
export const getWalletTransactions = () => client.get("/wallet/transactions");
