import { Component, OnInit } from "@angular/core";
import { AuthService } from "../../core/auth/services/auth.service";
import { User } from "../../core/auth/models/user.model";
import { ToastrService } from "ngx-toastr";

@Component({
  selector: "app-profile",
  templateUrl: "./profile.component.html",
  styleUrls: ["./profile.component.scss"],
})
export class ProfileComponent implements OnInit {
  user!: User;
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

  constructor(
    private authService: AuthService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getCurrentUser();
    this.formData = {
      name: this.user?.name || "",
      email: this.user?.email || "",
      address: this.user?.address || "",
      phone: this.user?.phone || "",
      bio: this.user?.bio || "",
    };
  }

  saveProfile() {
    this.authService.updateProfile(this.formData);
    this.toastr.success("Profil mis à jour avec succès !");
  }

  toggleNotification(key: keyof typeof this.notifications) {
    this.notifications[key] = !this.notifications[key];
  }

  saveNotifications() {
    console.log(
      "Préférences de notification mises à jour :",
      this.notifications
    );
    this.toastr.success("Préférences sauvegardées");
  }
}
