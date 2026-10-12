import { Baby, Bone, Brain, HeartPulse, Microscope, Smile, Sparkles } from "lucide-react";

// The hospital's departments as the public site shows them (home page, treatments, doctors). key is the doctors'
// speciality; bookable: false for a department whose doctors aren't in the app (visits arranged by phone).
export const DEPARTMENTS = [
  {
    key: "CARDIOLOGY",
    label: "Cardiology",
    icon: HeartPulse,
    summary: "Heart health, from check-ups to cardiac care.",
    treats: ["Chest pain and palpitations", "High blood pressure", "Heart check-ups"],
  },
  {
    key: "NEUROLOGY",
    label: "Neurology",
    icon: Brain,
    summary: "Brain, spine and nerve conditions.",
    treats: ["Headaches and migraine", "Seizures", "Numbness, weakness and dizziness"],
  },
  {
    key: "ORTHOPEDICS",
    label: "Orthopedics",
    icon: Bone,
    summary: "Bones, joints and sports injuries.",
    treats: ["Fractures and sprains", "Joint and back pain", "Arthritis"],
  },
  {
    key: "PEDIATRICS",
    label: "Pediatrics",
    icon: Baby,
    summary: "Care for babies, children and teenagers.",
    treats: ["Check-ups as children grow", "Fever, coughs and infections", "Childhood allergies"],
  },
  {
    key: "DERMATOLOGY",
    label: "Dermatology",
    icon: Sparkles,
    summary: "Skin, hair and nail conditions.",
    treats: ["Acne and eczema", "Hair and scalp problems", "Rashes and skin infections"],
  },
  {
    key: "DENTISTRY",
    label: "Dentistry",
    icon: Smile,
    summary: "Check-ups, fillings and dental care.",
    treats: ["Tooth pain and sensitivity", "Gum problems", "Dental check-ups"],
  },
  {
    key: "ONCOLOGY",
    label: "Oncology",
    icon: Microscope,
    summary: "Cancer diagnosis and treatment.",
    treats: ["Screening and diagnosis", "Treatment planning", "Follow-up care"],
    bookable: false,
  },
];

export const BOOKABLE_DEPARTMENTS = DEPARTMENTS.filter((department) => department.bookable !== false);

export const departmentOf = (speciality) => DEPARTMENTS.find((department) => department.key === speciality) ?? null;
