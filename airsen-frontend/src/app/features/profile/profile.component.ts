import { Component, OnInit } from "@angular/core";
import { AuthService } from "../../core/auth/services/auth.service";
import { AuthUser } from "../../core/auth/models/auth.model";
import { UserProfileService } from "./services/user-profile.service";
import { UserDTO } from "./models/user.model";
import { UpdateUserProfileRequest } from "./models/update-user-profile-request.model";
import { MatDialog } from '@angular/material/dialog';
import { ProfileDialogComponent } from './profile-dialog.component';



@Component({
  selector: "app-profile",
  templateUrl: "./profile.component.html",
  styleUrls: ["./profile.component.scss"],
  standalone: false,
})
export class ProfileComponent implements OnInit {
  user!: AuthUser;
  formData = {
    firstName: "",
    lastName: "",
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
  // En attente de MAJ du backend 
  // userStats = {
  //   joinDate: "Mars 2024",
  //   discussions: 23,
  //   likes: 145,
  //   followers: 67,
  // };

  activeTab: "profile" | "notifications" | "security" = "profile";

  constructor(
    private authService: AuthService,  
    private userProfileService: UserProfileService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
  this.userProfileService.getProfile().subscribe({
    next: (user: UserDTO) => {
      this.formData = {
        firstName: user.firstName || "",
        lastName: user.lastName || "",
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
        firstName: "",
        lastName: "",
        email: "",
        address: "",
        phone: "",
        bio: "",
      };
    }
  });
}

  saveProfile() {
  // Vérification du format du téléphone avant de lancer la requête
  const phonePattern = /^[0-9]*$/;
  if (!phonePattern.test(this.formData.phone)) {
    this.dialog.open(ProfileDialogComponent, {
      data: {
        title: 'Erreur',
        message: 'Le numéro de téléphone ne doit contenir que des chiffres.',
      },
    });
    return; // stope la sauvegarde ici
  }
  // Vérifie que le téléphone a au moins 10 caractères
  if (this.formData.phone.length < 10) {
    this.dialog.open(ProfileDialogComponent, {
      data: {
        title: 'Erreur',
        message: 'Le numéro de téléphone doit comporter au moins 10 chiffres.',
      },
    });
    return;
  }

  const updateData: UpdateUserProfileRequest = {
    firstName: this.formData.firstName,
    lastName: this.formData.lastName,
    address: this.formData.address,
    telephone: this.formData.phone,
    bio: this.formData.bio,
  };

  this.userProfileService.updateProfile(updateData).subscribe({
    next: (updatedUser) => {
      this.dialog.open(ProfileDialogComponent, {
        data: {
          title: 'Succès',
          message: 'Profil sauvegardé avec succès !'
        }
      });
      console.log("Profil mis à jour :", updatedUser);
    },
    error: (err) => {
      this.dialog.open(ProfileDialogComponent, {
        data: {
          title: 'Erreur',
          message: 'Erreur lors de la sauvegarde du profil.'
        }
      });
      console.error("Erreur lors de la sauvegarde du profil :", err);
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
  const first = this.formData.firstName || "";
  const last = this.formData.lastName || "";
  return (first[0] || "") + (last[0] || "");
}

}
