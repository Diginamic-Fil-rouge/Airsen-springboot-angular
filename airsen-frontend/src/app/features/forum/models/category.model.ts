import { Thread } from "./thread.model";

export interface Category {
  id: number;
  name: string;
  description: string;
  color: string;
  threads: Thread[];
}