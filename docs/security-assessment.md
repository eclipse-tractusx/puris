# Security Assessment PURIS (incl. Frontend, Backend Services, IAM and other infrastructure)

|                           |                                                                                                |
| ------------------------- | ---------------------------------------------------------------------------------------------- |
| Contact for product       | [@tom-rm-meyer-ISST](https://github.com/tom-rm-meyer-ISST) |
| Security responsible      | [@SSIRKC](https://github.com/SSIRKC) <br> [@szymonkowalczykzf](https://github.com/szymonkowalczykzf) |
| Version number of product | 23.12                                                                                          |
| Dates of assessment       | 2023-12-11: Re-Assessment                                                                      |
| Status of assessment      | RE-ASSESSMENT DONE                                                                            |

## Product Description
Application Security review provides information about application design, architecture and current security state.
The Predictive Unit Real-Time Information System (PURIS) KIT provides the capabilities to exchange the production related information within a given relationship between two business partners such as the already available stock of the supplier, the current stock of the customer and the customer's demand. The aim is to help to mitigate potential and actual shortage scenarios.

PURIS is the second of two components of the Demand and Capacity Management as approached by the Resiliency Kit:
A planning component forecasting its demands and capacities for the next months up to multiple years.
An operationalization component verifying the demands and capacities of its actual production plan from today up to multiple weeks.


Within the Business Domain Resiliency, the Demand and Capacity Management (DCM) represents the planning and the PURIS represents the operationalization component. Considering a shorter time period in PURIS based on the production plan, results in more reliable information.
Generally, the Demand and Capacity Management needs a close cooperation between a company and its partners along the supply chain.
Within the value chain, each partner needs to plan his own production and relies on his customers' demand forecasts. The bullwhip effect describes the rising amplified deviation between orders to suppliers and sales to buyers along the value chain. The fluctuation rises from tier to tier. Using the latest production related information, you can mitigate the bullwhip effect for you and your partners within the value chain.

As a customer, you can verify the production related information of your partner so that you can identify potential shortages earlier with less effort and mitigate or resolve them spending less resources.
As a supplier, you can increase your production efficiency, e.g. by optimizing your batch size based on your customers' latest demands.

Currently the PURIS Application have 1 main functionality:
Customers can View and Manage Stocks in the app.
Additionally, functionality related to the fully functional Customer dashboard is currently under creation.
The rest of the functionalities, especially the resiliency ones are currently  not yet implemented and only planned for the further development & implementation for upcoming future.

## Scope of the review
|ID | Component Description |
| ------------------------- | ------------------------- |
|1 | Vue User Interface (Frontend) |
|2 | Stock View Controller |
|3 |	H2 Database |
|4 |	Data Response Controller |
|5 |	Data Request Controller |
