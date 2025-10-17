import { User } from "@/app/core/auth/models/user.model"
import { Category } from "./category.model";
import { Message } from "./message.model";
import { Vote } from "./vote.model";

export interface Thread{
    id: number;
    title: string;
    content: string;
    author: User | null;
    category: Category | null;
    messages: Message[];
    votes: Vote[];
    createdDate: Date;
    lastMessageDate: Date;
    messageCount: number;
    viewCount: number;
    likeCount: number;
    closed: boolean;
    pinned: boolean;
}