## Project Installation

1. Clone the project
2. Make sure the PURIS backend and the product-edc is running with all its components
3. The `VITE_BASE_URL` in the `.env` file must be configured to the PURIS backend instance
4. Run the application:
    - (npm) Use `npm install` and `npm run dev` (for development) or `npm run build` (for production)
    - (Docker) Run `docker build .` and `docker run -d -p 3000:3000 CONTAINERID`
5. Done! The frontend should be available at `http://YOURIP:3000/`