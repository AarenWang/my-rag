/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        display: ["Fraunces", "Georgia", "serif"],
        body: ["Aptos", "Segoe UI", "sans-serif"]
      },
      colors: {
        ink: "#17211b",
        moss: "#355e3b",
        wheat: "#f1d7a5",
        paper: "#fff8e8"
      }
    }
  },
  plugins: []
};

