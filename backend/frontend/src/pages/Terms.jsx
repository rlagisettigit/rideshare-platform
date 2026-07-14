import LegalPageLayout from "../components/LegalPageLayout";
import TermsContent from "../components/legal/TermsContent";

export default function Terms() {
  return (
    <LegalPageLayout title="Terms & Conditions" lastUpdated="July 14, 2026">
      <TermsContent />
    </LegalPageLayout>
  );
}
