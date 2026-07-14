import Modal from "../Modal";
import TermsContent from "./TermsContent";
import PrivacyContent from "./PrivacyContent";

/** doc: "terms" | "privacy" */
export default function LegalModal({ doc, onClose }) {
  return (
    <Modal title={doc === "terms" ? "Terms & Conditions" : "Privacy Policy"} onClose={onClose} maxWidth={640}>
      <div className="legal-content" style={{ padding: 0 }}>
        {doc === "terms" ? <TermsContent /> : <PrivacyContent />}
      </div>
    </Modal>
  );
}
