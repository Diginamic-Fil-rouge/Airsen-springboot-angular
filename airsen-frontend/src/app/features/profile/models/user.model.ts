export interface UserDTO {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  address?: string;
  telephone?: string;
  bio?: string;
  role: string;
}
