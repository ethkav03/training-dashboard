import { z } from "zod";

export const googleMobileSchema = z.object({
  idToken: z.string().min(20),
});
