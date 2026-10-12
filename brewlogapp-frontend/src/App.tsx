import { lazy, Suspense } from 'react'
import { Routes, Route } from 'react-router'
import { CoffeeBeanDetailComponent } from './components/CoffeeBeanDetailComponent'
import { CoffeeBeanListComponent } from './components/CoffeeBeanListComponent'
import { FlavorNoteDetailComponent } from './components/FlavorNoteDetailComponent'
import { FlavorNoteListComponent } from './components/FlavorNoteListComponent'
import { BrewEntryDetailComponent } from './components/BrewEntryDetailComponent'
import { BrewEntryListComponent } from './components/BrewEntryListComponent'
import { HomeComponent } from './components/HomeComponent'
import { ExportComponent } from './components/ExportComponent'
import { Layout } from './components/Layout'
import { LoginComponent } from './components/LoginComponent'
import { OAuth2CallbackComponent } from './components/OAuth2CallbackComponent'
import { RegisterComponent } from './components/RegisterComponent'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AdminRoute } from './components/AdminRoute'
import { AdminComponent } from './components/AdminComponent'
import { AdminBeansComponent } from './components/AdminBeansComponent'
import { AdminBeanDetailComponent } from './components/AdminBeanDetailComponent'
import { AdminBrewsComponent } from './components/AdminBrewsComponent'
import { AdminBrewDetailComponent } from './components/AdminBrewDetailComponent'
import { AdminFlavorNotesComponent } from './components/AdminFlavorNotesComponent'
import { AdminFlavorNoteDetailComponent } from './components/AdminFlavorNoteDetailComponent'
import { AdminUsersComponent } from './components/AdminUsersComponent'
import { AdminUserDetailComponent } from './components/AdminUserDetailComponent'
import { AdminAuditComponent } from './components/AdminAuditComponent'
import './App.css'

// Lazy-loaded so recharts ships in a separate chunk, not the main bundle.
const AnalyticsComponent = lazy(() =>
  import('./components/AnalyticsComponent').then(m => ({ default: m.AnalyticsComponent })),
)
const AdminAnalyticsComponent = lazy(() =>
  import('./components/AdminAnalyticsComponent').then(m => ({ default: m.AdminAnalyticsComponent })),
)
// Lazy-loaded so Leaflet ships in a separate chunk, not the main bundle.
const CafeLocationListComponent = lazy(() =>
  import('./components/CafeLocationListComponent').then(m => ({ default: m.CafeLocationListComponent })),
)
const CafeLocationDetailComponent = lazy(() =>
  import('./components/CafeLocationDetailComponent').then(m => ({ default: m.CafeLocationDetailComponent })),
)
const AdminCafesComponent = lazy(() =>
  import('./components/AdminCafesComponent').then(m => ({ default: m.AdminCafesComponent })),
)
const AdminCafeDetailComponent = lazy(() =>
  import('./components/AdminCafeDetailComponent').then(m => ({ default: m.AdminCafeDetailComponent })),
)
// Lazy-loaded: the report detail view pulls in recharts.
const ReportListComponent = lazy(() =>
  import('./components/ReportListComponent').then(m => ({ default: m.ReportListComponent })),
)
const ReportDetailComponent = lazy(() =>
  import('./components/ReportDetailComponent').then(m => ({ default: m.ReportDetailComponent })),
)
const AdminReportsComponent = lazy(() =>
  import('./components/AdminReportsComponent').then(m => ({ default: m.AdminReportsComponent })),
)
const AdminReportDetailComponent = lazy(() =>
  import('./components/AdminReportDetailComponent').then(m => ({ default: m.AdminReportDetailComponent })),
)

const lazyRoute = (node: React.ReactNode) => (
  <Suspense fallback={<div className="empty">Loading…</div>}>{node}</Suspense>
)

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginComponent />} />
      <Route path="/register" element={<RegisterComponent />} />
      <Route path="/oauth2/callback" element={<OAuth2CallbackComponent />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<HomeComponent />} />
          <Route path="/beans" element={<CoffeeBeanListComponent />} />
          <Route path="/details/:id" element={<CoffeeBeanDetailComponent />} />
          <Route path="/flavor-notes" element={<FlavorNoteListComponent />} />
          <Route path="/flavor-notes/:id" element={<FlavorNoteDetailComponent />} />
          <Route path="/brews" element={<BrewEntryListComponent />} />
          <Route path="/brews/:id" element={<BrewEntryDetailComponent />} />
          <Route path="/export" element={<ExportComponent />} />
          <Route path="/cafes" element={lazyRoute(<CafeLocationListComponent />)} />
          <Route path="/cafes/:id" element={lazyRoute(<CafeLocationDetailComponent />)} />
          <Route path="/analytics" element={lazyRoute(<AnalyticsComponent />)} />
          <Route path="/reports" element={lazyRoute(<ReportListComponent />)} />
          <Route path="/reports/:id" element={lazyRoute(<ReportDetailComponent />)} />
          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<AdminComponent />} />
            <Route path="/admin/beans" element={<AdminBeansComponent />} />
            <Route path="/admin/beans/:id" element={<AdminBeanDetailComponent />} />
            <Route path="/admin/brews" element={<AdminBrewsComponent />} />
            <Route path="/admin/brews/:id" element={<AdminBrewDetailComponent />} />
            <Route path="/admin/flavor-notes" element={<AdminFlavorNotesComponent />} />
            <Route path="/admin/flavor-notes/:id" element={<AdminFlavorNoteDetailComponent />} />
            <Route path="/admin/cafes" element={lazyRoute(<AdminCafesComponent />)} />
            <Route path="/admin/cafes/:id" element={lazyRoute(<AdminCafeDetailComponent />)} />
            <Route path="/admin/users" element={<AdminUsersComponent />} />
            <Route path="/admin/users/:id" element={<AdminUserDetailComponent />} />
            <Route path="/admin/audit" element={<AdminAuditComponent />} />
            <Route path="/admin/analytics" element={lazyRoute(<AdminAnalyticsComponent />)} />
            <Route path="/admin/reports" element={lazyRoute(<AdminReportsComponent />)} />
            <Route path="/admin/reports/:id" element={lazyRoute(<AdminReportDetailComponent />)} />
          </Route>
        </Route>
      </Route>
    </Routes>
  )
}

export default App
