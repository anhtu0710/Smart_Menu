import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home.component';
import { DichVuComponent } from './features/DichVu/dichvu.component';
import { LienHeComponent } from './features/LienHe/lienhe.component';
import { LoginComponent } from './features/Auth/login/login.component';
import { RegisterComponent } from './features/Auth/register/register.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent
  },
  {
    path: 'dichvu',
    component: DichVuComponent,
    canActivate: [authGuard]
  },
  {
    path: 'service',
    redirectTo: 'dichvu',
    pathMatch: 'full'
  },
  {
    path: 'lienhe',
    component: LienHeComponent
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'register',
    component: RegisterComponent
  },
  {
    path: '**',
    redirectTo: ''
  }
];
