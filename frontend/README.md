<div align="center">
  <h2 align="center">PURIS Frontend</h2>
  The frontend of the Catena-X Predictive Unit Real-Time Information Service (PURIS)
</div>

## Table of Contents
- [Prerequirements](#prerequirements)
- [Getting Started](#getting-started)
- [License](#license)


## Prerequirements
The following things are needed to start PURIS:

- `npm` or an equivalent Docker setup
- A running PURIS backend instance


## Getting Started
1. Clone the project
2. Make sure the PURIS backend and the product-edc is running with all its components
3. The `VITE_BASE_URL` in the `.env` file must be configured to the PURIS backend instance
4. Run the application:
    - (npm) Use `npm install` and `npm run dev` (for development) or `npm run build` (for production)
    - (Docker) Run `docker build .` and `docker run -d -p 3000:3000 CONTAINERID`
5. Done! The frontend should be available at `http://YOURIP:3000/`


## License
The project is licensed under the [Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
For details on the licensing terms, see the `LICENSE` file.
