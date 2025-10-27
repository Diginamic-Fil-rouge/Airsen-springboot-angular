import { Component, OnInit } from "@angular/core";
import { AuthService } from "../../core/auth/services/auth.service";
import { AuthUser } from "../../core/auth/models/auth.model";

@Component({
  selector: "app-profile",
  templateUrl: "./profile.component.html",
  styleUrls: ["./profile.component.scss"],
  standalone: false,
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

  notifications: Record<string, boolean> = {
    emailAlerts: true,
    emergencyAlerts: true,
    forumReplies: true,
    weeklyReport: false,
  };

  notificationLabels: Record<keyof typeof this.notifications, { title: string; description: string }> = {
  emailAlerts: {
    title: "Alertes e-mail",
    description: "Recevoir des alertes environnementales par e-mail"
  },
  emergencyAlerts: {
    title: "Alertes d'urgence",
    description: "Alertes immédiates en cas de risque environnemental élevé"
  },
  forumReplies: {
    title: "Réponses du forum",
    description: "Être notifié des réponses à vos discussions"
  },
  weeklyReport: {
    title: "Rapport hebdomadaire",
    description: "Recevoir un résumé hebdomadaire de la qualité de l'air"
  }
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
