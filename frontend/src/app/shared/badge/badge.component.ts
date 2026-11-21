import { Component, input } from '@angular/core';
import { Badge } from '../../core/utils/labels';

@Component({
  selector: 'app-badge',
  template: `<span class="t" [class]="badge().tone">{{ badge().text }}</span>`,
})
export class BadgeComponent {
  readonly badge = input.required<Badge>();
}
