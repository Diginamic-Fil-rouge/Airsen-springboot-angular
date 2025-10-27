import { User } from "@/app/core/auth/models/user.model";
import { Thread } from "./thread.model";

export interface Vote{
    id: number;
    user: User;
    thread: Thread;
    voteType: string;
}