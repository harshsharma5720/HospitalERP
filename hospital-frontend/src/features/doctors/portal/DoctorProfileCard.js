import { Mail, Phone, Stethoscope } from "lucide-react";

export default function DoctorProfileCard({ doctor }) {
  return (
    <div className="sticky top-8 bg-white dark:bg-[#111a3b]
                    rounded-2xl shadow-xl p-6
                    border border-gray-100 dark:border-[#16224a]">

      <div className="text-center">
        <div className="w-24 h-24 mx-auto rounded-full
                        bg-gradient-to-r from-blue-600 to-cyan-400
                        flex items-center justify-center text-white
                        text-3xl font-bold">
          {doctor.name?.charAt(0)}
        </div>

        <h3 className="mt-4 text-xl font-bold dark:text-white">
          {doctor.name}
        </h3>

        <p className="text-sm text-gray-500 dark:text-[#8ddff8]">
          {doctor.specialist}
        </p>
      </div>

      <hr className="my-6 border-gray-200 dark:border-[#16224a]" />

      <div className="space-y-4 text-sm">
        <InfoRow icon={<Mail size={16} />} value={doctor.email} />
        <InfoRow icon={<Phone size={16} />} value={doctor.phoneNumber} />
        <InfoRow icon={<Stethoscope size={16} />} value={doctor.specialist} />
      </div>
    </div>
  );
}

function InfoRow({ icon, value }) {
  return (
    <div className="flex items-center gap-3 text-gray-700 dark:text-gray-300">
      <span className="text-blue-500">{icon}</span>
      <span>{value}</span>
    </div>
  );
}
