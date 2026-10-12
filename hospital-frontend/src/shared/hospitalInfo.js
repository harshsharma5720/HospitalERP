// The hospital as the public site shows it (header, home page, footer). One place to change the name or the
// contact details.
export const HOSPITAL = {
  name: "Shreya Hospital",
  phone: "+91 92895 20303",
  address: "Plot No. 837, Shalimar Garden Main Rd, Block C, Sahibabad, Ghaziabad, Uttar Pradesh 201006",
  area: "Sahibabad, Ghaziabad",
  hours: "Open 24×7",
};

export const phoneHref = `tel:${HOSPITAL.phone.replace(/\s/g, "")}`;

export const directionsHref = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${HOSPITAL.name}, ${HOSPITAL.address}`)}`;
