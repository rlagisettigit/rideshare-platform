import LegalPageLayout from "../components/LegalPageLayout";
import PrivacyContent from "../components/legal/PrivacyContent";

export default function PrivacyPolicy() {
  return (
    <LegalPageLayout title="Privacy Policy" lastUpdated="July 14, 2026">
      <PrivacyContent />
    </LegalPageLayout>
  );
}
