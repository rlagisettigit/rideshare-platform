import client from "./client";

export const previewRoutes = (payload) => client.post("/routes/preview", payload);
export const fetchRoutePlaces = (encodedPolyline) => client.post("/routes/preview/places", { encodedPolyline });
