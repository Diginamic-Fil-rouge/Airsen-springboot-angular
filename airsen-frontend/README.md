# Airsen Frontend - Air Quality Monitoring Platform

A modern application for monitoring air quality, weather data, and community discussions in France.

##  UI/UX Design Concepts & Recommendations

### Design Philosophy

#### 1. **Clean & Intuitive Dashboard Design**
- **Card-based Layout**: Use Angular Material cards for data visualization
- **Color-coded Indicators**: 
  - 🟢 Good (0-50): #4CAF50
  - 🟡 Moderate (51-100): #FF9800  
  - 🔴 Poor (101-150): #F44336
  - 🟣 Very Poor (151+): #9C27B0
- **Progressive Disclosure**: Show essential info first, details on demand
- **Real-time Updates**: Visual indicators for data freshness

#### 2. **Interactive Map Visualization**
- **Leaflet Integration**: OpenStreetMap with custom air quality markers
- **Clustered Markers**: Avoid visual clutter with marker clustering
- **Color-coded Pins**: Immediate visual feedback on air quality levels
- **Interactive Popups**: Rich information cards on marker click
- **Fallback Search**: Text-based commune search as backup (Option 2)

#### 3. **Responsive Mobile-First Design**
- **Progressive Enhancement**: Core functionality on all devices
- **Touch-friendly**: Minimum 44px touch targets
- **Optimized Charts**: Responsive chart.js implementation
- **Collapsible Navigation**: Space-efficient mobile navigation

#### 4. **Data Visualization Best Practices**
- **Temporal Charts**: Line charts for historical trends
- **Comparative Views**: Side-by-side air quality vs weather
- **Export Options**: PDF reports and CSV data export
- **Loading States**: Skeleton screens and progress indicators

### Key Features Implementation

####  **Home Page**
- Hero section with current location air quality
- Call-to-action for registration
- Testimonials 

####  **Dashboard** 
- Personal air quality overview
- Weather correlation charts
- Favorite locations summary
- Recent forum activity

####  **Interactive Map**
- Full-screen map with air quality overlay
- Search and filter by region/department
- Click markers for detailed information
- Legend for quality indicators

####  **Favorites Management**
- Quick access to saved locations
- Custom display names
- Default location setting
- One-click data access

####  **Historical Analysis**
- Date range picker (Material DatePicker)
- Multi-metric comparison charts
- Statistical summaries
- Data export functionality

####  **Community Forum**
- Categorized discussions
- Voting system (like/dislike)
- Moderation tools for admins
- Real-time notifications

####  **Notification System**
- In-app notification center
- Email alert preferences
- Geographic scope selection
- Alert history

### Technical Architecture

#### **State Management**
- Services with RxJS for reactive data flow
- Local storage for user preferences
- HTTP interceptors for auth and error handling

#### **Performance Optimization**
- Lazy loading for forum module
- OnPush change detection strategy
- Image optimization and caching
- Service worker for offline capability

#### **Accessibility**
- WCAG 2.1 AA compliance
- Keyboard navigation support
- Screen reader compatibility
- High contrast mode support

### Color Palette & Typography

#### **Primary Colors**
- Primary Blue: `#1976D2` (Material Blue 700)
- Accent Green: `#00E676` (Material Green A400)
- Warning: `#FF5722` (Material Deep Orange)

#### **Air Quality Colors**
```scss
$air-quality-colors: (
  good: #4CAF50,      // Green
  moderate: #FF9800,   // Orange
  poor: #F44336,      // Red
  very-poor: #9C27B0  // Purple
);
```

#### **Typography**
- Primary Font: Roboto (Google Fonts)
- Headings: Roboto Medium
- Body Text: Roboto Regular
- Code/Data: Roboto Mono

### Component Architecture

```
src/app/
├── components/
│   ├── layout/          # Header, Footer, Sidenav
│   ├── auth/           # Login, Register
│   ├── pages/          # Main page components
│   └── shared/         # Reusable components
├── services/           # Data services
├── models/            # TypeScript interfaces
├── guards/            # Route protection
├── interceptors/      # HTTP interceptors
└── utils/            # Helper functions
```

### Development Workflow

#### **Setup Commands**
```bash
# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build

# Run tests
npm test

# Lint code
npm run lint
```

#### **Environment Configuration**
- Development: `http://localhost:8080` (Spring Boot API)
- Production: Configure with actual API URL
- Map tokens: Configure Mapbox/Leaflet tokens

### Key Dependencies

#### **Core Angular**
- Angular 17+ with Material Design
- Reactive Forms for all user inputs
- HTTP Client with interceptors
- Router with guards

#### **UI/UX Libraries**
- Angular Material 17+
- Leaflet for maps
- Chart.js with ng2-charts
- Angular CDK for components

#### **Utilities**
- date-fns for date handling
- RxJS for reactive programming
- ngx-pagination for data tables

### Best Practices

#### **Code Quality**
- TypeScript strict mode
- ESLint + Prettier configuration
- Unit tests with Jasmine/Karma
- E2E tests with Cypress

#### **Performance**
- OnPush change detection
- TrackBy functions for *ngFor
- Image lazy loading
- Bundle optimization

#### **Security**
- JWT token management
- XSS protection
- Input sanitization
- HTTPS enforcement

##  Getting Started

1. **Install Node.js 20+**
2. **Clone repository**
3. **Install dependencies**: `npm install`
4. **Configure environment**: Update `src/environments/`
5. **Start development**: `npm start`
6. **Access application**: `http://localhost:4200`

##  Responsive Breakpoints

- Mobile: < 768px
- Tablet: 768px - 1024px
- Desktop: > 1024px

---

