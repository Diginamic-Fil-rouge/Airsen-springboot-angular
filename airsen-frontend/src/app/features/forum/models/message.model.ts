import { User } from "@/app/core/auth/models/user.model";
import { Thread } from "./thread.model";

export interface Message{
    id: number;
    content: string;
    author: User;
    createdDate: Date;
    thread: Thread;
}