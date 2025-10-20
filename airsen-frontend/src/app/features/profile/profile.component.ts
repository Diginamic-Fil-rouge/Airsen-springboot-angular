import { Component, OnInit } from "@angular/core";
import { AuthService } from "../../core/auth/services/auth.service";
import { AuthUser } from "../../core/auth/models/auth.model";

@Component({
  selector: "app-profile",
  templateUrl: "./profile.component.html",
  styleUrls: ["./profile.component.scss"],
  standalone : false
})
export class ProfileComponent implements OnInit {
  user!: AuthUser;
  formData = {
    name: "",
    email: "",
    address: "",
    phone: "",
    bio: "",
  };

  notifications = {
    emailAlerts: true,
    forumReplies: true,
    weeklyReport: false,
    emergencyAlerts: true,
  };

  userStats = {
    joinDate: "Mars 2024",
    discussions: 23,
    likes: 145,
    followers: 67,
  };

  activeTab: "profile" | "notifications" | "security" = "profile";

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();

    this.user = currentUser ?? {
      id: 0,
      firstName: "",
      lastName: "",
      email: "",
      role: "USER",
      address: "",
      phone: "",
      bio: "",
    };

    this.formData = {
      name: `${this.user.firstName} ${this.user.lastName}`,
      email: this.user.email,
      address: this.user.address || "",
      phone: this.user.phone || "",
      bio: this.user.bio || "",
    };
  }

  saveProfile() {
    console.log("Profil sauvegardé:", this.formData);
    alert("Profil sauvegardé (simulation)");
  }

  toggleNotification(key: keyof typeof this.notifications) {
    this.notifications[key] = !this.notifications[key];
  }

  // Wrapper pour contourner le problème de cast dans le template
  toggleNotificationKey(key: string) {
    this.toggleNotification(key as keyof typeof this.notifications);
  }

  saveNotifications() {
    console.log("Notifications sauvegardées :", this.notifications);
    alert("Notifications sauvegardées (simulation)");
  }
}
