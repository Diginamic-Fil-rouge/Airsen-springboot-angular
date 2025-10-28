import { Component, OnInit } from "@angular/core";
import { AuthService } from "../../core/auth/services/auth.service";
import { AuthUser } from "../../core/auth/models/auth.model";
import { UserProfileService } from "./services/user-profile.service";
import { UpdateUserProfileRequest } from "./models/update-user-profile-request.model";
import { User } from "@/auth/models/user.model";


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

  constructor(private authService: AuthService,  private userProfileService: UserProfileService) {}

  ngOnInit(): void {
  this.userProfileService.getProfile().subscribe({
    next: (user: User) => {
      this.formData = {
        name: `${user.firstName} ${user.lastName}`,
        email: user.email,
        address: user.address || "",
        phone: user.telephone || "",
        bio: user.bio || "",
      };
    },
    error: (err) => {
      console.error("Erreur lors de la récupération du profil :", err);
      alert("Impossible de récupérer le profil.");
      // fallback temporaire
      this.formData = {
        name: "",
        email: "",
        address: "",
        phone: "",
        bio: "",
      };
    }
  });
}

  saveProfile() {
  // Séparer le nom complet en firstName / lastName
  const [firstName, ...rest] = this.formData.name.split(" ");
  const lastName = rest.join(" ") || "";

  const updateData: UpdateUserProfileRequest = {
    firstName,
    lastName,
    address: this.formData.address,
    telephone: this.formData.phone,
    bio: this.formData.bio,
  };

  this.userProfileService.updateProfile(updateData).subscribe({
    next: (updatedUser) => {
      alert("Profil sauvegardé avec succès !");
      console.log("Profil mis à jour :", updatedUser);
    },
    error: (err) => {
      console.error("Erreur lors de la sauvegarde du profil :", err);
      alert("Erreur lors de la sauvegarde du profil.");
    }
  });
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

  getInitials(): string {
  if (!this.formData.name) return "";
  const names = this.formData.name.trim().split(" ");
  const initials = names.map(n => n[0].toUpperCase()).slice(0, 2).join("");
  return initials;
}

}
