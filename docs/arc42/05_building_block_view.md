# Building Block View

The components or (sub-) systems do have the following capabilities. Please note that the authentication flows have 
been omitted for readability.

| Component / system                 | Descriptions                                                                                                                                                                                                                              |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Data Provisioning & Transformation | The Data Provisioning & Transformation Building Block handles the upload of data from internal systems into PURIS and provides capabilities for data transformation. **This component is not part of this repository**.                   |
| PURIS FOSS                         | This system represents the PURIS FOSS application. It handles the data exchange and visualization.                                                                                                                                        |
| EDC                                | The Eclipse Dataspace Components Connector (EDC) is the component allowing PURIS FOSS to participate in the IDS. It is used to provide and consume data assets following policy information. Any data transfer is routed through the EDC. |

## Level 1 White Boxes

No whitebox is needed for the "EDC" and for "Data Provisioning & Data Transformation".

| Component / system | Descriptions                                                                                            |
|--------------------|---------------------------------------------------------------------------------------------------------|
| Frontend           | The frontend provides the capabilities to interact with the data.                                       |
| Backend            | The backend provides the capabilities to interact with the EDC, store data and handle the data exchange |


## Level 2 White Boxes

**Frontend**

The Frontend only handles visualization logic. The remaining logic is handled in the backend.

| Component / system | Descriptions                                                                                                                                                 |
|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Stock View         | Allows to manually add or update stock information that is allocated to partners. Also latest stock information for partners may be requested (via backend). |
| Dashboard          | The dashboard allows to compare material-related demands, production outputs and stocks in a mocked way. Only Stock information is currently implements.     |

**Backend**

| Component / system | Descriptions                                                                                                                                            |
|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| EDC Adapter        | The EDC Adapter handles the asset creation, negotiation and transfer intiialization.                                                                    |
| Master Data        | Stores the Partner and material related information. They may only be added via REST interfaces.                                                        |
| Stock | Stores and handles stock related data. It provides interfaces to create and read stock data. Also it allows to exchanged stock information via the EDC. |
