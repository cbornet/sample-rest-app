import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IOrder } from 'app/shared/model/order.model';

type EntityResponseType = HttpResponse<IOrder>;
type EntityArrayResponseType = HttpResponse<IOrder[]>;

@Injectable({ providedIn: 'root' })
export class OrderService {
  public resourceUrl = SERVER_API_URL + 'api/orders';

  constructor(protected http: HttpClient) {}

  create(order: IOrder): Observable<EntityResponseType> {
    return this.http.post<IOrder>(this.resourceUrl, order, { observe: 'response' });
  }

  update(order: IOrder): Observable<EntityResponseType> {
    return this.http.put<IOrder>(this.resourceUrl, order, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IOrder>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IOrder[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }
}
