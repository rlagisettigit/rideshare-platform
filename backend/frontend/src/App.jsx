import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import TopNav from "./components/TopNav";
import Home from "./pages/Home";
import LandingPage from "./pages/LandingPage";
import Login from "./pages/Login";
import Register from "./pages/Register";
import CompleteProfile from "./pages/CompleteProfile";
import RideSearch from "./pages/RideSearch";
import RidePublish from "./pages/RidePublish";
import RecurringRides from "./pages/RecurringRides";
import RecurringRideActivity from "./pages/RecurringRideActivity";
import MyBookings from "./pages/MyBookings";
import EditProfile from "./pages/EditProfile";
import DriverDashboard from "./pages/DriverDashboard";
import AdminDashboard from "./pages/AdminDashboard";
import Notifications from "./pages/Notifications";
import Payments from "./pages/Payments";
import Ratings from "./pages/Ratings";

function AppShell({ children }) {
  return (
    <div className="app-shell">
      <TopNav />
      <main className="main-content">{children}</main>
    </div>
  );
}

function Protected({ children, roles }) {
  return (
    <ProtectedRoute roles={roles}>
      <AppShell>{children}</AppShell>
    </ProtectedRoute>
  );
}

function RootRedirect() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Protected><Home /></Protected> : <LandingPage />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/complete-profile"
            element={
              <ProtectedRoute requireCompleteProfile={false}>
                <CompleteProfile />
              </ProtectedRoute>
            }
          />
          <Route path="/search" element={<Protected><RideSearch /></Protected>} />
          <Route path="/publish" element={<Protected roles={["DRIVER"]}><RidePublish /></Protected>} />
          <Route path="/recurring-rides" element={<Protected><RecurringRideActivity /></Protected>} />
          <Route path="/recurring-rides/publish" element={<Protected roles={["DRIVER"]}><RecurringRides /></Protected>} />
          <Route path="/bookings" element={<Protected><MyBookings /></Protected>} />
          <Route path="/profile" element={<Protected><EditProfile /></Protected>} />
          <Route path="/notifications" element={<Protected><Notifications /></Protected>} />
          <Route path="/payments" element={<Protected><Payments /></Protected>} />
          <Route path="/ratings" element={<Protected><Ratings /></Protected>} />
          <Route path="/driver" element={<Protected roles={["DRIVER"]}><DriverDashboard /></Protected>} />
          <Route path="/admin" element={<Protected roles={["ADMIN"]}><AdminDashboard /></Protected>} />
          <Route path="/" element={<RootRedirect />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
